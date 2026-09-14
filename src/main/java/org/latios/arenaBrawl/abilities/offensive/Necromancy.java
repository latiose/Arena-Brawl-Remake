package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.EnergyManager;

import java.util.List;

public class Necromancy implements Ability {

    private final double energyCost;
    private final double arrowDamage;
    private final int skeletonHitsToKill;
    private final AbilityCost cost;
    private final SkeletonEntityManager skeletonManager;

    public Necromancy(EnergyManager energyManager, SkeletonEntityManager skeletonManager, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.arrowDamage = config.getDouble("arrow-damage", 50.0);
        this.skeletonHitsToKill = config.getInt("skeleton-hits-to-kill", 6);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.skeletonManager = skeletonManager;
    }

    @Override
    public String getName() { return "Necromancy"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Summons a Skeleton archer that shoots arrows at nearby enemies dealing "
                + (int) arrowDamage + " damage per arrow.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Arrow Damage", String.valueOf((int) arrowDamage)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Skeleton Hits", String.valueOf(skeletonHitsToKill))
        );
    }

    @Override
    public boolean activate(Player player) {
        skeletonManager.summonSkeleton(player, null);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1.0, 0), 20, 0.4, 0.4, 0.4, 0.05);
        return true;
    }
}