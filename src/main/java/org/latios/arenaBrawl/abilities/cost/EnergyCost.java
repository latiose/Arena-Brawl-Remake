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

    /** Cost after any active offensive energy discount (see {@link OffensiveEnergyDiscount}). */
    private double getEffectiveCost(Player player) {
        return energyAmount * OffensiveEnergyDiscount.getMultiplier(player);
    }

    @Override
    public boolean canPay(Player player) {
        return energyManager.hasEnough(player, getEffectiveCost(player));
    }

    @Override
    public void pay(Player player) {
        energyManager.consume(player, getEffectiveCost(player));
    }

    @Override
    public String describeRemaining(Player player) {
        return "You don't have enough energy";
    }

    @Override
    public String getBaseCostDescription() {
        return (int) energyAmount + " energy";
    }
}
