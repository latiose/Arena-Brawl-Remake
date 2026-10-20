package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Targeted taunt inspired by Rammus' E. The caster marks an enemy within a short range and, for the
 * duration, that enemy is forced to auto-attack the caster with melee hits while in melee range.
 *
 * <p>Minecraft does not let a server control another player's input, so the forced attacks are
 * simulated: the taunted player swings and a regular "Melee" hit is resolved through
 * {@link CombatService}, respecting shields, Counter, Thornheart and the normal melee cooldown.</p>
 */
public class Taunt implements Ability {

    public record TauntState(UUID casterId, long endMs) {}

    private static final Map<UUID, TauntState> ACTIVE_TAUNTS = new ConcurrentHashMap<>();

    /** Returns the active taunt applied to this player, or {@code null} if none/expired. */
    public static TauntState getActive(Player player) {
        TauntState state = ACTIVE_TAUNTS.get(player.getUniqueId());
        if (state == null) return null;

        if (System.currentTimeMillis() >= state.endMs()) {
            ACTIVE_TAUNTS.remove(player.getUniqueId(), state);
            return null;
        }
        return state;
    }

    public static void clear(Player player) {
        ACTIVE_TAUNTS.remove(player.getUniqueId());
    }

    private final long cooldownMs;
    private final long durationMs;
    private final double maxRange;
    private final double meleeRange;
    private final long attackIntervalMs;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CooldownManager cooldownManager;
    private final CombatUpgradeManager combatUpgradeManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public Taunt(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                 CombatUpgradeManager combatUpgradeManager, CombatService combatService,
                 DebuffManager debuffManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 20000L);
        this.durationMs = config.getLong("duration-ms", 2000L);
        this.maxRange = config.getDouble("max-range", 3.0);
        this.meleeRange = config.getDouble("melee-range", 3.5);
        this.attackIntervalMs = config.getLong("attack-interval-ms", 500L);

        this.cost = new CooldownCost(cooldownManager, "taunt", cooldownMs, combatUpgradeManager);
        this.cooldownManager = cooldownManager;
        this.combatUpgradeManager = combatUpgradeManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.teamManager = teamManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Taunt";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Taunts an enemy within " + (int) maxRange + " blocks for " + (durationMs / 1000.0)
                + "s. They are immobilized and forced to attack you with melee hits.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Duration", (durationMs / 1000.0) + "s"),
                new AbilityStat("Immobilize", (durationMs / 1000.0) + "s"),
                new AbilityStat("Forced Hits", "every " + (attackIntervalMs / 1000.0) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);
        if (target == null) {
            target = findClosestInCone(player);
        }

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        final Player taunted = target;
        UUID targetId = taunted.getUniqueId();
        UUID casterId = player.getUniqueId();
        TauntState state = new TauntState(casterId, System.currentTimeMillis() + durationMs);
        ACTIVE_TAUNTS.put(targetId, state);

        // Root the target for the duration so it cannot walk out of melee range.
        debuffManager.tryApply(player, taunted, DebuffType.IMMOBILIZE, durationMs);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 10.0f, 1.1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 10.0f, 0.8f);
        taunted.getWorld().playSound(taunted.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 10.0f, 1.2f);

        player.sendMessage(MessageUtils.positive() + String.format(
                "§3You taunted §c%s§3 for §e%.1fs§3!",
                taunted.getName(), durationMs / 1000.0
        ));
        taunted.sendMessage(MessageUtils.negative() + String.format(
                "§3You are taunted by §c%s§3!",
                player.getName()
        ));

        new BukkitRunnable() {
            long lastForcedHit = 0L;

            @Override
            public void run() {
                TauntState active = ACTIVE_TAUNTS.get(targetId);

                if (active == null || System.currentTimeMillis() >= active.endMs()
                        || !taunted.isOnline() || taunted.isDead()
                        || !player.isOnline() || player.isDead()) {
                    ACTIVE_TAUNTS.remove(targetId, state);
                    cancel();
                    return;
                }

                drawLink(taunted, player);
                taunted.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                        taunted.getLocation().add(0, 2.1, 0), 1, 0.2, 0.1, 0.2, 0.0);

                long now = System.currentTimeMillis();
                if (now - lastForcedHit < attackIntervalMs) return;

                if (canForcedAttack(taunted, player)) {
                    performForcedAttack(taunted, player);
                    lastForcedHit = now;
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private boolean canForcedAttack(Player attacker, Player victim) {
        if (attacker.isDead() || victim.isDead()) return false;
        if (attacker.getGameMode() == GameMode.SPECTATOR || victim.getGameMode() == GameMode.SPECTATOR) return false;
        if (debuffManager.hasDebuff(attacker, DebuffType.STUN)) return false;
        if (cooldownManager.isOnCooldown(attacker, "melee_hit")) return false;

        double rangeSq = meleeRange * meleeRange;
        if (attacker.getLocation().distanceSquared(victim.getLocation()) > rangeSq) return false;
        return attacker.hasLineOfSight(victim);
    }

    private void performForcedAttack(Player attacker, Player victim) {
        double damage = combatUpgradeManager.getValue(attacker, CombatUpgradeType.MELEE_DAMAGE);

        attacker.swingMainHand();

        combatService.applyAbilityDamage(attacker, victim, damage, "Melee");
        cooldownManager.setCooldown(attacker, "melee_hit", attackIntervalMs);
    }

    private void drawLink(Player target, Player caster) {
        Location from = target.getEyeLocation();
        Location to = caster.getEyeLocation();
        Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / 8.0);

        Location point = from.clone();
        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.0f);

        for (int i = 0; i < 8; i++) {
            point.add(step);
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0.0, 0.0, 0.0, 0.0, red);
        }
    }

    /** Fallback for short range: nearest enemy in a generous cone in front of the caster. */
    private Player findClosestInCone(Player player) {
        Vector look = player.getEyeLocation().getDirection().normalize();
        double bestDistance = Double.MAX_VALUE;
        Player best = null;

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || other.isDead() || !other.isOnline()) continue;
            if (other.getGameMode() == GameMode.SPECTATOR) continue;
            if (teamManager.isAlly(player, other)) continue;

            double distance = other.getLocation().distance(player.getLocation());
            if (distance > maxRange) continue;

            Vector toTarget = other.getEyeLocation().toVector()
                    .subtract(player.getEyeLocation().toVector());
            if (toTarget.lengthSquared() == 0) continue;
            if (look.dot(toTarget.normalize()) < 0.5) continue;
            if (!player.hasLineOfSight(other)) continue;

            if (distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }

        return best;
    }
}
