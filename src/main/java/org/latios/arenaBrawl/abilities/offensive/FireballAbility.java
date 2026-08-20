package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.AbilityItemKeys;
import org.latios.arenaBrawl.general.EnergyManager;

public class FireballAbility implements Ability {

    private final AbilityCost cost;
    private static final double DAMAGE = 105.0;
    private static final double ENERGY_COST = 40.0;

    public FireballAbility(EnergyManager energyManager) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
    }

    @Override
    public String getName() { return "Fireball"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(0f);
        fireball.setIsIncendiary(false);

        fireball.getPersistentDataContainer().set(
                AbilityItemKeys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE, DAMAGE
        );
        fireball.getPersistentDataContainer().set(
                AbilityItemKeys.PROJECTILE_SOURCE_ABILITY, PersistentDataType.STRING, getName()
        );
        return true;
    }
}