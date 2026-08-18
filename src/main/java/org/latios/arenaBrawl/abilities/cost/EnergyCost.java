package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.entity.Player;

import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.general.EnergyManager;

public class EnergyCost implements AbilityCost {

    private final EnergyManager energyManager;
    private final double energyAmount;

    public EnergyCost(EnergyManager energyManager, double energyAmount) {
        this.energyManager = energyManager;
        this.energyAmount = energyAmount;
    }

    @Override
    public boolean canPay(Player player) {
        return energyManager.hasEnough(player, energyAmount);
    }

    @Override
    public void pay(Player player) {
        energyManager.consume(player, energyAmount);
    }

    @Override
    public String describeRemaining(Player player) {
        return (int) energyManager.getEnergy(player) + "/" + (int) EnergyManager.MAX_ENERGY + " energy";
    }
}