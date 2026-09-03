package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.ShieldManager;

import java.util.List;

public class ShieldWall implements Ability {

    private final double damageReduction;
    private final long durationMillis;
    private final long chargeTimeMillis;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final ShieldManager shieldManager;

    public ShieldWall(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                      ShieldManager shieldManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.shieldManager = shieldManager;

        this.damageReduction = config.getDouble("damage-reduction", 0.70);
        this.durationMillis = config.getLong("duration-ms", 10000L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);

        this.cost = new UltimateCost(cooldownManager, usageManager, "shieldwall");
    }

    @Override
    public String getName() { return "Shield Wall"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "shieldwall", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        shieldManager.applyShield(player, damageReduction, durationMillis,"Shield Wall");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.0f);

        spawnWaterRings(player);

        new BukkitRunnable() {
            int secondsElapsed = 0;
            int maxSeconds = (int) (durationMillis / 1000L);

            @Override
            public void run() {
                if (!player.isOnline() || secondsElapsed >= maxSeconds || !shieldManager.hasShield(player)) {
                    cancel();
                    return;
                }

                player.getWorld().spawnParticle(
                        Particle.DRAGON_BREATH,
                        player.getLocation().add(0, 1.0, 0),
                        15,
                        0.5, 0.5, 0.5, 0.1,
                        1.0f
                );

                secondsElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private void spawnWaterRings(Player player) {
        Location loc = player.getLocation();
        int circleCount = 8;
        int pointsPerCircle = 15;
        double radius = 1.3;

        double minHeight = 0.2;
        double maxHeight = 2.2;
        double heightStep = (maxHeight - minHeight) / (circleCount - 1);

        for (int i = 0; i < circleCount; i++) {
            double yOffset = minHeight + (i * heightStep);

            for (int j = 0; j < pointsPerCircle; j++) {
                double angle = (2 * Math.PI / pointsPerCircle) * j;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location particleLoc = loc.clone().add(x, yOffset, z);
                player.getWorld().spawnParticle(Particle.DRIPPING_WATER, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Reduces incoming damage by a big amount for some time.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage Reduction", (int) (damageReduction * 100) + "%"),
                new AbilityStat("Duration", (durationMillis / 1000) + "s"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}