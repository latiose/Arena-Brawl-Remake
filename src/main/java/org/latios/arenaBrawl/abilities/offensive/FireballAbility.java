package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;

import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;

import org.latios.arenaBrawl.abilities.cost.EnergyCost;

import org.latios.arenaBrawl.general.EnergyManager;

import java.util.List;

public class FireballAbility implements Ability {

    private final AbilityCost cost;
    private static final double DAMAGE = 105.0;
    private static final double ENERGY_COST = 40.0;

    private static final double AOE_RADIUS = 3.0; // blocks

    public FireballAbility(EnergyManager energyManager) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
    }

    @Override
    public String getName() { return "Fireball"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Fireball fireball = AbilityProjectileFactory.launchAoe(
                player, Fireball.class, DAMAGE, getName(), AOE_RADIUS
        );
        fireball.setYield(0f);
        fireball.setIsIncendiary(false);
        return true;
    }

    @Override
    public String getDescription() {
        return "Launches a fireball, dealing damage to enemies in an area";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("AoE Radius", AOE_RADIUS + " blocks"),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + ""),
                new AbilityStat("Range", "Unlimited")
        );
    }
}