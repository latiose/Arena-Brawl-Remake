package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MeleeHitEffect;

public class ScavengerMeleeEffect implements MeleeHitEffect {

    private final ScavengerManager scavengerManager;
    private final EnergyManager energyManager;
    private final AbilityConfig config;
    public ScavengerMeleeEffect(ScavengerManager scavengerManager, EnergyManager energyManager, AbilityConfig config) {
        this.scavengerManager = scavengerManager;
        this.energyManager = energyManager;
        this.config = config;
    }

    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        if (!scavengerManager.isActive(attacker)) return;

        energyManager.addEnergy(attacker, config.getDouble("energy-per-hit", 10.0));

        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        attacker.getWorld().spawnParticle(Particle.CRIT, impactLocation, 12, 0.3, 0.3, 0.3, 0.1);
        attacker.getWorld().spawnParticle(Particle.FIREWORK, attacker.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
    }

    @Override
    public double getMultiplier(Player attacker) {
        return 1.0;
    }
}