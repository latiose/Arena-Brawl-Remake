package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MeleeHitEffect;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.ShieldManager;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultimate. Thunder binds the caster in armor: 20% less incoming damage and,
 * on every melee hit in either direction, a 20% chance to stun the enemy for
 * one second.
 */
public class ThunderArmor implements Ability, MeleeHitEffect {

    private record State(long endMs, double stunChance, long stunMillis) {}

    private static final Map<UUID, State> ACTIVE = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private static final String SHIELD_TITLE = "THUNDER ARMOR";

    private final Plugin plugin;
    private final long durationMillis;
    private final double damageReduction;
    private final double stunChance;
    private final long stunMillis;
    private final long chargeTimeMillis;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final CooldownManager cooldownManager;
    private final AbilityCost cost;

    public ThunderArmor(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                        ShieldManager shieldManager, DebuffManager debuffManager, AbilityConfig config) {
        this.plugin = plugin;
        this.durationMillis = config.getLong("duration-ms", 16000L);
        this.damageReduction = config.getDouble("damage-reduction", 0.20);
        this.stunChance = config.getDouble("stun-chance", 0.20);
        this.stunMillis = config.getLong("stun-ms", 1000L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "thunderarmor");
    }

    @Override
    public String getName() {
        return "Thunder Armor";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "thunderarmor", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "You are struck with lightning, gaining " + Math.round(damageReduction * 100)
                + "% damage reduction for " + (durationMillis / 1000L) + "s. Every melee hit in either "
                + "direction has a " + Math.round(stunChance * 100) + "% chance to stun your enemy for "
                + (stunMillis / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage Reduction", Math.round(damageReduction * 100) + "%"),
                new AbilityStat("Stun Chance", Math.round(stunChance * 100) + "% per melee hit"),
                new AbilityStat("Stun Duration", (stunMillis / 1000L) + "s"),
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000L) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        UUID id = player.getUniqueId();
        ACTIVE.put(id, new State(System.currentTimeMillis() + durationMillis, stunChance, stunMillis));

        Location loc = player.getLocation();
        player.getWorld().strikeLightningEffect(loc);
        player.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10.0f, 1.0f);
        player.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);

        shieldManager.applyShield(player, damageReduction, durationMillis, SHIELD_TITLE);
        player.sendMessage(MessageUtils.positive() + "§3Thunder Armor surrounds you with lightning!");

        Location body = loc.clone().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, body, 40, 0.4, 0.8, 0.4, 0.15);
        player.getWorld().spawnParticle(Particle.END_ROD, body, 20, 0.5, 0.9, 0.5, 0.05);

        long maxTicks = durationMs() / 50L;

        new BukkitRunnable() {
            long elapsedTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || elapsedTicks >= maxTicks
                        || !ACTIVE.containsKey(id)) {
                    cancel();
                    return;
                }

                if (elapsedTicks % 4 == 0) {
                    Location sparkBody = player.getLocation().add(0, 1.0, 0);
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, sparkBody,
                            8, 0.35, 0.6, 0.35, 0.05);
                }

                elapsedTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private long durationMs() {
        return durationMillis;
    }

    /** Removes the armor state (match end / death cleanup). */
    public static void clear(Player player) {
        ACTIVE.remove(player.getUniqueId());
    }

    /** Called by CombatService after the wearer's own melee hit lands. */
    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        State state = activeState(attacker.getUniqueId());
        if (state == null) return;

        if (roll(state)) {
            stun(attacker, victim, state);
        }
    }

    /**
     * Called by CombatService after a melee hit lands on a wearer of
     * Thunder Armor: the enemy attacker may instead be stunned.
     */
    public static void onMeleeDamageTaken(Player victim, Player attacker, DebuffManager debuffManager) {
        State state = activeState(victim.getUniqueId());
        if (state == null) return;

        if (RANDOM.nextDouble() < state.stunChance()) {
            debuffManager.tryApply(victim, attacker, DebuffType.STUN, state.stunMillis());
        }
    }

    @Override
    public double getMultiplier(Player attacker) {
        return 1.0;
    }

    private static State activeState(UUID id) {
        State state = ACTIVE.get(id);
        if (state == null) return null;
        if (System.currentTimeMillis() >= state.endMs()) {
            ACTIVE.remove(id, state);
            return null;
        }
        return state;
    }

    private boolean roll(State state) {
        return RANDOM.nextDouble() < state.stunChance();
    }

    private void stun(Player roller, Player target, State state) {
        debuffManager.tryApply(roller, target, DebuffType.STUN, state.stunMillis());
    }
}
