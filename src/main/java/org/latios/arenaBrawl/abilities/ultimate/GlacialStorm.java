package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class GlacialStorm implements Ability {

    private final Plugin plugin;
    private final long chargeTimeMillis;
    private final double maxRange;
    private final double radius;
    private final int durationSeconds;
    private final double damagePerTick;
    private final int tickIntervalTicks;
    private final long slowDurationMs;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public GlacialStorm(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                        TeamManager teamManager, CombatService combatService, DebuffManager debuffManager,
                        AbilityConfig config) {
        this.plugin = plugin;
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.maxRange = config.getDouble("max-range", 18.0);
        this.radius = config.getDouble("radius", 6.0);
        this.durationSeconds = Math.max(1, config.getInt("duration-seconds", 6));
        this.damagePerTick = config.getDouble("damage-per-tick", 20.0);
        this.tickIntervalTicks = Math.max(1, config.getInt("tick-interval-ticks", 5));
        this.slowDurationMs = config.getLong("slow-duration-ms", 400L);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "glacialstorm");
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Glacial Storm";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "glacialstorm", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        Location center = resolveCenter(player);
        int totalTicks = durationSeconds * 20;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 0.6f);
        center.getWorld().playSound(center, Sound.BLOCK_SNOW_BREAK, 1.2f, 0.7f);
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 0.5, 0),
                60, radius * 0.5, 0.6, radius * 0.5, 0.03);

        new BukkitRunnable() {
            int elapsedTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)) {
                    cancel();
                    return;
                }

                if (elapsedTicks >= totalTicks) {
                    dissipate(center);
                    cancel();
                    return;
                }

                pulse(player, center);
                elapsedTicks += tickIntervalTicks;
            }
        }.runTaskTimer(plugin, 0L, tickIntervalTicks);

        return true;
    }

    private Location resolveCenter(Player player) {
        RayTraceResult result = player.rayTraceBlocks(maxRange);
        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition().toLocation(player.getWorld());
        }

        Vector direction = player.getEyeLocation().getDirection().normalize();
        return player.getEyeLocation().clone().add(direction.multiply(maxRange));
    }

    private void pulse(Player caster, Location center) {
        drawStorm(center);

        double radiusSquared = radius * radius;
        for (Player target : config.getMatchManager().getMatchPlayers(caster)) {
            if (target.equals(caster) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (target.getLocation().distanceSquared(center) > radiusSquared) continue;

            debuffManager.tryApply(caster, target, DebuffType.SLOW, slowDurationMs);
            combatService.applyAbilityDamage(caster, target, damagePerTick, getName(), center);

            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1.0, 0),
                    8, 0.3, 0.5, 0.3, 0.02);
        }
    }

    private void drawStorm(Location center) {
        int points = 28;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);

            point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 1, 0.05, 0.05, 0.05, 0.01);
            if (i % 4 == 0) {
                point.getWorld().spawnParticle(Particle.CLOUD, point, 1, 0.1, 0.2, 0.1, 0.01);
            }
        }

        center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0, 0.5, 0),
                10, radius * 0.35, 0.4, radius * 0.35, 0.02);
        center.getWorld().playSound(center, Sound.BLOCK_SNOW_BREAK, 0.6f, 1.3f);
    }

    private void dissipate(Location center) {
        center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 0.5, 0),
                50, radius * 0.5, 0.6, radius * 0.5, 0.05);
        center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0, 0.5, 0),
                25, radius * 0.4, 0.5, radius * 0.4, 0.03);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.4f);
    }

    @Override
    public String getDescription() {
        return "Casts a glacial storm at the target location for " + durationSeconds
                + "s, damaging enemies inside every " + (tickIntervalTicks * 50L) + "ms and slowing them.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage per tick", String.valueOf((int) damagePerTick)),
                new AbilityStat("Tick Interval", (tickIntervalTicks * 50L) + "ms"),
                new AbilityStat("Duration", durationSeconds + "s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}
