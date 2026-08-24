package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeType;

public class CooldownCost implements AbilityCost {

    private final CooldownManager cooldownManager;
    private final String abilityKey;
    private final long baseCooldownMillis;
    private final CombatUpgradeManager upgradeManager;

    public CooldownCost(CooldownManager cooldownManager, String abilityKey, long baseCooldownMillis,
                        CombatUpgradeManager upgradeManager) {
        this.cooldownManager = cooldownManager;
        this.abilityKey = abilityKey;
        this.baseCooldownMillis = baseCooldownMillis;
        this.upgradeManager = upgradeManager;
    }

    @Override
    public boolean canPay(Player player) {
        return !cooldownManager.isOnCooldown(player, abilityKey);
    }

    @Override
    public void pay(Player player) {
        double reductionPercent = upgradeManager.getValue(player, CombatUpgradeType.COOLDOWN_REDUCTION);
        long effectiveCooldown = (long) (baseCooldownMillis * (1 - reductionPercent / 100.0));
        cooldownManager.setCooldown(player, abilityKey, effectiveCooldown);
    }

    @Override
    public String describeRemaining(Player player) {
        return cooldownManager.getRemainingSeconds(player, abilityKey) + "s";
    }

    @Override
    public int getRemainingSeconds(Player player) {
        return (int) cooldownManager.getRemainingSeconds(player, abilityKey);
    }

    @Override
    public String getBaseCostDescription() {
        return (baseCooldownMillis / 1000) + "s cooldown";
    }
}