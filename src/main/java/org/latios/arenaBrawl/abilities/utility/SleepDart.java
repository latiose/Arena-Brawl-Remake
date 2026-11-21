package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.ProjectileInterceptionService;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A skillshot that puts the first enemy hit to sleep. The victim wakes up when
 * the sleep expires or as soon as they take damage.
 */
public class SleepDart implements Ability {

    private record SleepWatch(Object token, BukkitTask task) {
    }

    private static final Map<UUID, SleepWatch> ACTIVE = new ConcurrentHashMap<>();

    /** Cancels the wake monitor for this player. The debuff itself is cleared by the debuff system. */
    public static void clear(Player player) {
        SleepWatch watch = ACTIVE.remove(player.getUniqueId());
        if (watch != null) {
            watch.task().cancel();
        }
    }

    private final long cooldownMs;
    private final long sleepDurationMs;
    private final double maxRange;
    private final double step;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public SleepDart(Plugin plugin, CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                     TeamManager teamManager, DebuffManager debuffManager, PlayerHealthManager healthManager,
                     AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 20_000L);
        this.sleepDurationMs = Math.max(500L, config.getLong("sleep-duration-ms", 3_000L));
        this.maxRange = config.getDouble("max-range", 20.0);
        this.step = Math.max(0.3, config.getDouble("step", 0.9));

        this.plugin = plugin;
        this.cost = new CooldownCost(cooldownManager, "sleepdart", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Sleep Dart";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location start = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = start.getDirection().normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.4f);

        new BukkitRunnable() {
            private Location current = start.clone();
            private double travelled = 0;

            @Override
            public void run() {
                Location previous = current.clone();
                current.add(direction.clone().multiply(step));
                travelled += step;

                if (ProjectileInterceptionService.tryIntercept(player, previous, current)) {
                    cancel();
                    return;
                }

                current.getWorld().spawnParticle(Particle.END_ROD, current, 1, 0, 0, 0, 0);

                if (current.getBlock().getType().isSolid()) {
                    puff(current);
                    cancel();
                    return;
                }

                for (Player target : current.getWorld().getPlayers()) {
                    if (!isValidTarget(player, target)) continue;

                    if (target.getBoundingBox().expand(0.4, 0.4, 0.4)
                            .contains(current.getX(), current.getY(), current.getZ())) {
                        putToSleep(player, target);
                        cancel();
                        return;
                    }
                }

                if (travelled >= maxRange) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private boolean isValidTarget(Player caster, Player target) {
        if (target.equals(caster) || target.isDead()) return false;
        if (target.getGameMode() == GameMode.SPECTATOR) return false;
        if (!config.getMatchManager().isInMatch(target)) return false;
        return teamManager.isEnemy(caster, target);
    }

    private void putToSleep(Player caster, Player victim) {
        Location center = victim.getLocation().add(0, 1.6, 0);

        boolean applied = debuffManager.tryApply(caster, victim, DebuffType.SLEEP, sleepDurationMs);
        if (!applied) {
            puff(center);
            return;
        }

        center.getWorld().spawnParticle(Particle.CLOUD, center, 15, 0.3, 0.2, 0.3, 0.01);
        center.getWorld().spawnParticle(Particle.ENCHANT, center, 12, 0.4, 0.3, 0.4, 0.2);
        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);

        startWakeMonitor(victim, healthManager.getHealth(victim));
    }

    private void startWakeMonitor(Player victim, double baselineHealth) {
        UUID victimId = victim.getUniqueId();
        Object token = new Object();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                SleepWatch watch = ACTIVE.get(victimId);
                if (watch == null || watch.token() != token) {
                    cancel();
                    return;
                }

                if (!victim.isOnline() || victim.isDead()
                        || victim.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(victim)
                        || !debuffManager.hasDebuff(victim, DebuffType.SLEEP)) {
                    ACTIVE.remove(victimId);
                    cancel();
                    return;
                }

                // Taking any real damage wakes the victim immediately.
                if (healthManager.getHealth(victim) < baselineHealth - 0.5) {
                    debuffManager.clearIfType(victim, DebuffType.SLEEP);
                    Location head = victim.getLocation().add(0, 1.6, 0);
                    head.getWorld().spawnParticle(Particle.CRIT, head, 10, 0.3, 0.2, 0.3, 0.1);
                    head.getWorld().playSound(head, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 1.4f);
                    ACTIVE.remove(victimId);
                    cancel();
                    return;
                }

                Location head = victim.getLocation().add(0, 1.8, 0);
                head.getWorld().spawnParticle(Particle.CLOUD, head, 1, 0.15, 0.05, 0.15, 0.01);
            }
        }.runTaskTimer(plugin, 2L, 2L);

        ACTIVE.put(victimId, new SleepWatch(token, task));
    }

    private void puff(Location location) {
        location.getWorld().spawnParticle(Particle.CLOUD, location, 8, 0.2, 0.2, 0.2, 0.01);
        location.getWorld().playSound(location, Sound.ENTITY_ITEM_BREAK, 0.5f, 1.6f);
    }

    @Override
    public String getDescription() {
        return "Fires a dart that puts the first enemy hit to sleep for "
                + (sleepDurationMs / 1000.0) + "s. Taking damage wakes them up.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Sleep", (sleepDurationMs / 1000.0) + "s"),
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
