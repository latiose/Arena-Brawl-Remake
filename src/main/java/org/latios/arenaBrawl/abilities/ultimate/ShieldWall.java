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

import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.ShieldManager;

import java.util.List;

public class ShieldWall implements Ability {

    private static final double DAMAGE_REDUCTION = 0.70;
    private static final long DURATION_MILLIS = 10_000;
    private static final long CHARGE_TIME_MILLIS = 60_000;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final ShieldManager shieldManager;

    public ShieldWall(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager, ShieldManager shieldManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "shieldwall");
        this.shieldManager = shieldManager;
    }

    @Override
    public String getName() { return "Shield Wall"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "shieldwall", CHARGE_TIME_MILLIS);
    }

    @Override
    public boolean activate(Player player) {
        shieldManager.applyShield(player, DAMAGE_REDUCTION, DURATION_MILLIS);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.0f);

        spawnWaterRings(player);

        new BukkitRunnable() {
            int secondsElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || secondsElapsed >= 10 || !shieldManager.hasShield(player)) {
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
        return "Reduces incoming damage by a big amount for some time";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage Reduction", (int) (DAMAGE_REDUCTION * 100) + "%"),
                new AbilityStat("Duration", (DURATION_MILLIS / 1000) + "s"),
                new AbilityStat("Charge Time", (CHARGE_TIME_MILLIS / 1000) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}