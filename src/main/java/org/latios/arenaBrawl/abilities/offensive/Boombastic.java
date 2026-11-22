package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Grants a short burst of speed, then detonates the caster for area damage.
 * The explosion is purely cosmetic plus ability damage: it never breaks or
 * modifies blocks.
 */
public class Boombastic implements Ability {

    /**
     * The pending detonation is scheduled outside the ability instance, so it
     * must be tracked globally to be cancellable on death / match end.
     */
    public record ActiveBomb(Object token, BukkitTask task, SpeedBuffManager speed, String speedSource) {
    }

    public static final Map<UUID, List<ActiveBomb>> ACTIVE = new ConcurrentHashMap<>();

    private static ActiveBomb find(UUID playerId, Object token) {
        List<ActiveBomb> bombs = ACTIVE.get(playerId);
        if (bombs == null) return null;
        for (ActiveBomb bomb : bombs) {
            if (bomb.token() == token) return bomb;
        }
        return null;
    }

    private static void unregister(UUID playerId, ActiveBomb bomb) {
        ACTIVE.computeIfPresent(playerId, (id, bombs) -> {
            bombs.remove(bomb);
            return bombs.isEmpty() ? null : bombs;
        });
    }
    /**
     * Cancels the pending explosion and the speed boost. Called on death, match
     * end, /hub and disconnect so the blast never lands in the lobby or the
     * next match.
     */
    public static void clear(Player player) {
        List<ActiveBomb> bombs = ACTIVE.remove(player.getUniqueId());
        if (bombs == null) return;

        for (ActiveBomb bomb : bombs) {
            bomb.task().cancel();
            bomb.speed().removeBuff(player, bomb.speedSource());
        }
    }

    private static final int TICK_STEP = 4;

    private final double energyCost;
    private final int speedAmplifier;
    private final int speedDurationTicks;
    private final double damage;
    private final double radius;

    private final AbilityCost cost;
    private final SpeedBuffManager speedBuffManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityConfig config;

    public Boombastic(EnergyManager energyManager, TeamManager teamManager, CombatService combatService,
                      SpeedBuffManager speedBuffManager, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.speedDurationTicks = Math.max(1, config.getInt("speed-duration-ticks", 100));
        this.damage = config.getDouble("damage", 180.0);
        this.radius = config.getDouble("radius", 4.0);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.speedBuffManager = speedBuffManager;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Boombastic";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        UUID playerId = player.getUniqueId();

        Object token = new Object();
        String speedSource = "boombastic:" + UUID.randomUUID();

        speedBuffManager.applyBuff(player, speedAmplifier, speedDurationTicks * 50L, speedSource);

        Location start = player.getLocation().add(0, 1, 0);
        start.getWorld().playSound(start, Sound.ENTITY_TNT_PRIMED, 1.2f, 1.4f);
        start.getWorld().spawnParticle(Particle.SMOKE, start, 15, 0.35, 0.4, 0.35, 0.02);
        start.getWorld().spawnParticle(Particle.FLAME, start, 6, 0.3, 0.3, 0.3, 0.02);

        BukkitTask fuse = new BukkitRunnable() {
            int elapsedTicks = 0;

            @Override
            public void run() {
                ActiveBomb self = find(playerId, token);
                if (self == null) {
                    cancel();
                    return;
                }

                elapsedTicks += TICK_STEP;

                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)) {
                    clear(player); // cancels every bomb of this player
                    return;
                }

                Location feet = player.getLocation().add(0, 1, 0);

                if (elapsedTicks >= speedDurationTicks) {
                    unregister(playerId, self);
                    cancel();
                    speedBuffManager.removeBuff(player, speedSource);
                    detonate(player);
                    return;
                }

                boolean aboutToBlow = speedDurationTicks - elapsedTicks <= 20;
                feet.getWorld().spawnParticle(Particle.SMOKE, feet, aboutToBlow ? 8 : 3,
                        0.25, 0.3, 0.25, 0.01);
                feet.getWorld().spawnParticle(Particle.FLAME, feet, aboutToBlow ? 4 : 1,
                        0.2, 0.25, 0.2, 0.01);

                if (aboutToBlow && elapsedTicks % 8 == 0) {
                    feet.getWorld().playSound(feet, Sound.UI_BUTTON_CLICK, 1.0f, 1.6f);
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), TICK_STEP, TICK_STEP);

        ACTIVE.computeIfAbsent(playerId, id -> new CopyOnWriteArrayList<>())
                .add(new ActiveBomb(token, fuse, speedBuffManager, speedSource));

        return true;
    }

    private void detonate(Player caster) {
        Location center = caster.getLocation().add(0, 0.5, 0);

        for (Player target : config.getMatchManager().getMatchPlayers(caster)) {
            if (target.equals(caster) || target.isDead()
                    || target.getGameMode() == GameMode.SPECTATOR
                    || !teamManager.isEnemy(caster, target)) {
                continue;
            }

            if (target.getLocation().distanceSquared(center) > radius * radius) continue;

            combatService.applyAbilityDamage(caster, target, damage, getName(), center);
        }

        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.FLAME, center, 40, radius * 0.4, 0.5, radius * 0.4, 0.06);
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 30, radius * 0.4, 0.5, radius * 0.4, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
    }

    @Override
    public String getDescription() {
        return "Gives you a burst of speed. When it ends you explode, dealing area damage to nearby "
                + "enemies without breaking any blocks.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) damage + " HP"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Speed duration", (speedDurationTicks / 20) + "s"),
                new AbilityStat("Speed Level", String.valueOf(speedAmplifier + 1)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
