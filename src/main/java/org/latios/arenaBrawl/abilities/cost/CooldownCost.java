package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;

public class CooldownCost implements AbilityCost {
    private final CooldownManager cooldownManager;
    private final String abilityKey;
    private final long cooldownMillis;

    public CooldownCost(CooldownManager cooldownManager, String abilityKey, long cooldownMillis) {
        this.cooldownManager = cooldownManager;
        this.abilityKey = abilityKey;
        this.cooldownMillis = cooldownMillis;
    }

    @Override
    public boolean canPay(Player player) {
        return !cooldownManager.isOnCooldown(player, abilityKey);
    }

    @Override
    public void pay(Player player) {
        cooldownManager.setCooldown(player, abilityKey, cooldownMillis);
    }

    @Override
    public String describeRemaining(Player player) {
        return cooldownManager.getRemainingSeconds(player, abilityKey) + "s";
    }

    @Override
    public int getRemainingSeconds(Player player) {
        return (int) cooldownManager.getRemainingSeconds(player, abilityKey);
    }
}
