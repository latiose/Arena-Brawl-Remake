package org.latios.arenaBrawl.abilities.support;

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
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class HealingRain implements Ability {

    private final long cooldownMs;
    private final double healPerSecond;
    private final double radius;
    private final int durationSeconds;
    private final double cloudHeight;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public HealingRain(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                       PlayerHealthManager healthManager, CombatUpgradeManager combatUpgradeManager,
                       AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.healPerSecond = config.getDouble("heal-per-second", 50.0);
        this.radius = config.getDouble("radius", 4.0);
        this.durationSeconds = config.getInt("duration-seconds", 6);
        this.cloudHeight = config.getDouble("cloud-height", 3.5);

        this.cost = new CooldownCost(cooldownManager, "healingrain", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() {
        return "Healing Rain";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location groundCenter = player.getLocation().getBlock().getLocation().add(0.5, 0.1, 0.5);
        Location cloudCenter = groundCenter.clone().add(0, cloudHeight, 0);

        groundCenter.getWorld().playSound(groundCenter, Sound.WEATHER_RAIN, 1.0f, 1.2f);
        groundCenter.getWorld().playSound(groundCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);

        new BukkitRunnable() {
            private int ticksRun = 0;
            private final int totalTicks = durationSeconds * 20;

            @Override
            public void run() {
                ticksRun += 2;

                if (ticksRun >= totalTicks) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 6; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 1.2;
                    double offsetZ = (Math.random() - 0.5) * radius * 1.2;
                    double offsetY = (Math.random() - 0.5) * 0.4;
                    cloudCenter.getWorld().spawnParticle(Particle.CLOUD, cloudCenter.clone().add(offsetX, offsetY, offsetZ), 1, 0, 0, 0, 0);
                }

                for (int i = 0; i < 4; i++) {
                    double dropX = (Math.random() - 0.5) * radius * 1.8;
                    double dropZ = (Math.random() - 0.5) * radius * 1.8;
                    Location rainStart = cloudCenter.clone().add(dropX, 0, dropZ);
                    rainStart.getWorld().spawnParticle(Particle.FALLING_WATER, rainStart, 2, 0.1, 0.5, 0.1, 0);
                }

                if (ticksRun % 20 == 0) {
                    groundCenter.getWorld().playSound(groundCenter, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.4f, 1.8f);

                    for (Player ally : groundCenter.getWorld().getNearbyEntitiesByType(Player.class, groundCenter, radius, cloudHeight, radius)) {
                        if (ally.isDead() || teamManager.isEnemy(player, ally)) continue;

                        Location allyLoc = ally.getLocation();
                        double distance2D = Math.hypot(allyLoc.getX() - groundCenter.getX(), allyLoc.getZ() - groundCenter.getZ());
                        boolean inVerticalRange = allyLoc.getY() >= groundCenter.getY() - 1.0 && allyLoc.getY() <= cloudCenter.getY();

                        if (distance2D <= radius && inVerticalRange) {
                            if (ally.equals(player)) {
                                healthManager.heal(player, healPerSecond, getName());
                            } else {
                                healthManager.healAlly(player, ally, healPerSecond, getName());
                            }
                            ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().add(0, 1.0, 0), 5, 0.2, 0.4, 0.2, 0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Summons a stationary rain cloud that heals you and nearby allies inside it over " + durationSeconds + " seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal/sec", (int) healPerSecond + " HP"),
                new AbilityStat("Duration", durationSeconds + "s"),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}