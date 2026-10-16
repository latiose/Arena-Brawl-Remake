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

import static org.latios.arenaBrawl.general.ParticleUtils.spawnBloodEffect;

public class BerserkMeleeEffect implements MeleeHitEffect {

    private static final int DRIP_WAVE_COUNT = 4;
    private static final int DRIP_PARTICLES_PER_WAVE = 6;

    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        if (!Berserk.BERSERK_ACTIVE_PLAYERS.contains(attacker.getUniqueId())) return;
        Location impact2 = impactLocation.clone().subtract(0, 1.2, 0);
        spawnBloodEffect(impact2);
        spawnFallingDrips(victim, impactLocation);

        victim.getWorld().playSound(impactLocation, Sound.BLOCK_STONE_BREAK, 7.0f, 2f);
    }

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