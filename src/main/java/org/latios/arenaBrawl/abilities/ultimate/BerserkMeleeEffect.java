package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.MeleeHitEffect;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BerserkMeleeEffect implements MeleeHitEffect {

    private static final int SPRAY_PARTICLE_COUNT = 18;
    private static final int DRIP_WAVE_COUNT = 4;
    private static final int DRIP_PARTICLES_PER_WAVE = 6;

    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        if (!Berserk.BERSERK_ACTIVE_PLAYERS.contains(attacker.getUniqueId())) return;

        Vector hitDirection = victim.getLocation().toVector()
                .subtract(attacker.getLocation().toVector())
                .setY(0.15) // slight upward bias so the spray doesn't hug the ground immediately
                .normalize();

        spawnDirectionalSpray(impactLocation, hitDirection);
        spawnFallingDrips(victim, impactLocation);

        victim.getWorld().playSound(impactLocation, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
    }

    /**
     * Emits a cone of blood particles bursting away from the attacker's direction,
     * mimicking a directional splatter instead of a spherical cloud.
     */
    private void spawnDirectionalSpray(Location origin, Vector direction) {
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < SPRAY_PARTICLE_COUNT; i++) {
            Vector spread = direction.clone();

            // Add a random cone spread of roughly +/- 25 degrees around the hit direction
            spread.add(new Vector(
                    (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.4,
                    (random.nextDouble() - 0.5) * 0.6
            )).normalize();

            double speed = 0.15 + random.nextDouble() * 0.25;
            Location particleLoc = origin.clone().add(spread.clone().multiply(0.2));

            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(140, 0, 0), 1.3f);

            particleLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    0, // count = 0 so we control exact motion via offset/extra below
                    spread.getX() * speed, spread.getY() * speed, spread.getZ() * speed,
                    1.0, // extra = speed multiplier applied along the offset vector for DUST-type particles
                    dust
            );
        }
    }

    /**
     * Simulates blood dripping and falling for a few ticks after the initial splatter,
     * with each wave spawning slightly lower to fake gravity without real particle physics.
     */
    private void spawnFallingDrips(Player victim, Location impactLocation) {
        new BukkitRunnable() {
            int wave = 0;

            @Override
            public void run() {
                if (wave >= DRIP_WAVE_COUNT || victim.isDead()) {
                    cancel();
                    return;
                }

                Random random = ThreadLocalRandom.current();
                Location dripCenter = impactLocation.clone().subtract(0, wave * 0.25, 0);

                for (int i = 0; i < DRIP_PARTICLES_PER_WAVE; i++) {
                    Location dripLoc = dripCenter.clone().add(
                            (random.nextDouble() - 0.5) * 0.5,
                            (random.nextDouble() - 0.3) * 0.3,
                            (random.nextDouble() - 0.5) * 0.5
                    );

                    Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(140, 0, 0), 1.0f);
                    dripLoc.getWorld().spawnParticle(Particle.DUST, dripLoc, 1, 0, 0, 0, 0, dust);
                }

                wave++;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 1L, 2L); // 4 waves over ~8 ticks
    }


    @Override
    public double getMultiplier(Player attacker) {
        if (Berserk.BERSERK_ACTIVE_PLAYERS.contains(attacker.getUniqueId())) {
            return 2.0;
        }
        return 1.0;
    }
}