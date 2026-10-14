package org.latios.arenaBrawl.general;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ParticleUtils {

    public static void spawnBloodEffect(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        Location center = loc.clone().add(0, 1.0, 0);

        for (int i = 0; i < 100; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.6;
            double offsetY = (random.nextDouble() - 0.5) * 0.8;
            double offsetZ = (random.nextDouble() - 0.5) * 0.6;

            Location particleLoc = center.clone().add(offsetX, offsetY, offsetZ);

            world.spawnParticle(Particle.BLOCK, particleLoc, 10, 0.1, 0.1, 0.1, 0.15, org.bukkit.Material.REDSTONE_WIRE.createBlockData()
            );
        }
    }
}
