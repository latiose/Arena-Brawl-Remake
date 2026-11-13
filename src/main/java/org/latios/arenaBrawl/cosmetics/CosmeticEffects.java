package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Small helpers shared by the cosmetic hit and kill effects.
 */
final class CosmeticEffects {

    private CosmeticEffects() {
    }

    static void sound(Location location, Sound sound, float volume, float pitch) {
        World world = location.getWorld();
        if (world == null) return;
        world.playSound(location, sound, volume, pitch);
    }

    static void dust(Location location, int count, double spread, Color color, float size) {
        World world = location.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.DUST, location, count, spread, spread, spread, 0.0,
                new Particle.DustOptions(color, size));
    }

    static void ring(Location center, Particle particle, double radius, int points) {
        ring(center, particle, radius, points, 0.0);
    }

    static void ring(Location center, Particle particle, double radius, int points, double yOffset) {
        World world = center.getWorld();
        if (world == null) return;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            Location point = center.clone().add(
                    Math.cos(angle) * radius,
                    yOffset,
                    Math.sin(angle) * radius
            );
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    static void later(Plugin plugin, long delayTicks, Runnable action) {
        new BukkitRunnable() {
            @Override
            public void run() {
                action.run();
            }
        }.runTaskLater(plugin, delayTicks);
    }

    static double random(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    static Color randomColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
}
