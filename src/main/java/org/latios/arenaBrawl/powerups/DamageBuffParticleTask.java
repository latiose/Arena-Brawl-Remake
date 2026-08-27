
package org.latios.arenaBrawl.powerups;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DamageBuffParticleTask extends BukkitRunnable {

    private static final double RADIUS = 0.8;
    private static final int PARTICLE_COUNT = 12;

    private final DamageBuffManager damageBuffManager;

    public DamageBuffParticleTask(DamageBuffManager damageBuffManager) {
        this.damageBuffManager = damageBuffManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (damageBuffManager.getMultiplier(player) <= 1.0) continue; // no active buff, skip

            spawnFireRing(player);
        }
    }

    private void spawnFireRing(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = (2 * Math.PI * i) / PARTICLE_COUNT;
            double x = Math.cos(angle) * RADIUS;
            double z = Math.sin(angle) * RADIUS;

            Location particleLoc = center.clone().add(x, 0.1, z);
            world.spawnParticle(Particle.LAVA, center.clone().add(0, 0.1, 0), 2, 0.3, 0.1, 0.3, 0);
        }

    }
}