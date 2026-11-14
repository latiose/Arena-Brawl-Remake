package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.entity.Player;

import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.general.EnergyManager;

public class EnergyCost implements AbilityCost {

    private final EnergyManager energyManager;
    private final double energyAmount;

    public EnergyCost(EnergyManager energyManager, double energyAmount) {
        this.energyManager = energyManager;
        this.energyAmount = energyAmount;
    }

    /** Cost after any active offensive energy discount (see {@link OffensiveEnergyDiscount}). */
    private double getEffectiveCost(Player player, AbilitySlot slot) {
        double categoryMultiplier = slot != null ? AbilityCostReduction.getMultiplier(player, slot) : 1.0;
        return energyAmount * OffensiveEnergyDiscount.getMultiplier(player) * categoryMultiplier;
    }

    protected void consume(Player player, double amount) {
        energyManager.consume(player, amount);
    }

    @Override
    public boolean canPay(Player player) {
        return energyManager.hasEnough(player, getEffectiveCost(player, null));
    }

    @Override
    public void pay(Player player) {
        consume(player, getEffectiveCost(player, null));
    }

    @Override
    public boolean canPay(Player player, AbilitySlot slot) {
        return energyManager.hasEnough(player, getEffectiveCost(player, slot));
    }

    @Override
    public void pay(Player player, AbilitySlot slot) {
        consume(player, getEffectiveCost(player, slot));
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
