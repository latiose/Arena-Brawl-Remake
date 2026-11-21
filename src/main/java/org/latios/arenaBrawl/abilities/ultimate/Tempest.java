package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns the caster into a walking storm: for a few seconds a damaging aura
 * follows them, and it collapses with a final lightning burst.
 */
public class Tempest implements Ability {

    private record ActiveStorm(Object token, BukkitTask task) {
    }

    private static final Map<UUID, ActiveStorm> ACTIVE = new ConcurrentHashMap<>();

    /** Cancels a running storm, for example when the caster dies or the match ends. */
    public static void clear(Player player) {
        ActiveStorm storm = ACTIVE.remove(player.getUniqueId());
        if (storm != null) {
            storm.task().cancel();
        }
    }

    private final long chargeTimeMillis;
    private final int durationSeconds;
    private final double radius;
    private final double damagePerSecond;
    private final double finalBurstDamage;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityConfig config;

    public Tempest(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                   TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60_000L);
        this.durationSeconds = Math.max(1, config.getInt("duration-seconds", 8));
        this.radius = config.getDouble("radius", 7.0);
        this.damagePerSecond = config.getDouble("damage-per-second", 40.0);
        this.finalBurstDamage = config.getDouble("final-burst-damage", 250.0);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "tempest");
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Tempest";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "tempest", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        clear(player);

        UUID playerId = player.getUniqueId();
        Object token = new Object();

        BukkitTask task = new BukkitRunnable() {
            int pulsesLeft = durationSeconds;

            @Override
            public void run() {
                ActiveStorm storm = ACTIVE.get(playerId);
                if (storm == null || storm.token() != token) {
                    cancel();
                    return;
                }

                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)) {
                    ACTIVE.remove(playerId);
                    cancel();
                    return;
                }

                Location center = player.getLocation().add(0, 1.0, 0);
                damageNearby(player, center, damagePerSecond, true);
                drawStorm(center);

                pulsesLeft--;
                if (pulsesLeft <= 0) {
                    finalBurst(player, center);
                    ACTIVE.remove(playerId);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        ACTIVE.put(playerId, new ActiveStorm(token, task));

        Location start = player.getLocation().add(0, 1.0, 0);
        start.getWorld().playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 1.1f);
        start.getWorld().spawnParticle(Particle.CLOUD, start, 40, radius * 0.5, 1.0, radius * 0.5, 0.05);

        return true;
    }

    private void damageNearby(Player caster, Location center, double amount, boolean silent) {
        for (Player target : config.getMatchManager().getMatchPlayers(caster)) {
            if (target.equals(caster) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (target.getLocation().distanceSquared(center) > radius * radius) continue;

            if (silent) {
                combatService.applyAbilityDamage(caster, target, amount, getName(), center);
            } else {
                combatService.applyAbilityDamage(caster, target, amount, getName(), center);
            }
        }
    }

    private void drawStorm(Location center) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i;
            Location point = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

            point.getWorld().spawnParticle(Particle.CLOUD, point, 1, 0.1, 0.1, 0.1, 0.01);
            if (i % 4 == 0) {
                point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.1, 0.5, 0.1, 0.02);
            }
        }

        center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.4f);
    }

    private void finalBurst(Player caster, Location center) {
        damageNearby(caster, center, finalBurstDamage, false);

        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 80, radius * 0.5, 0.8, radius * 0.5, 0.2);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 50, radius * 0.5, 0.8, radius * 0.5, 0.05);
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.6f, 1.0f);
    }

    @Override
    public String getDescription() {
        return "For " + durationSeconds + "s a storm follows you, damaging nearby enemies every second. "
                + "When it ends, it strikes for a large burst.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage per second", (int) damagePerSecond + " HP"),
                new AbilityStat("Final burst", (int) finalBurstDamage + " HP"),
                new AbilityStat("Duration", durationSeconds + "s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}
