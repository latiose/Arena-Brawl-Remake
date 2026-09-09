package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.MeleeHitEffect;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BerserkMeleeEffect implements MeleeHitEffect {

    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        if (!Berserk.BERSERK_ACTIVE_PLAYERS.contains(attacker.getUniqueId())) return;

        BlockData redstoneData = Material.REDSTONE_BLOCK.createBlockData();
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < 35; i++) {
            Location particleLoc = impactLocation.clone().add(
                    (random.nextDouble() - 0.5) * 0.7,
                    random.nextDouble() * 0.8,
                    (random.nextDouble() - 0.5) * 0.7
            );

            double x = (random.nextDouble() - 0.5) * 1.4;
            double y = 0.5 + random.nextDouble() * 1.2;
            double z = (random.nextDouble() - 0.5) * 1.4;

            victim.getWorld().spawnParticle(Particle.BLOCK, particleLoc, 0, x, y, z, 0.35, redstoneData);
        }

        victim.getWorld().playSound(impactLocation, Sound.BLOCK_STONE_BREAK, 1.0f, 1.1f);
    }

    @Override
    public double getMultiplier(Player attacker) {
        if (Berserk.BERSERK_ACTIVE_PLAYERS.contains(attacker.getUniqueId())) {
            return 2.0;
        }
        return 1.0;
    }
}