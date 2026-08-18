
package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;

public class UltimateCost implements AbilityCost {

    private final CooldownManager cooldownManager;
    private final UsageManager usageManager;
    private final String abilityKey;

    public UltimateCost(CooldownManager cooldownManager, UsageManager usageManager, String abilityKey) {
        this.cooldownManager = cooldownManager;
        this.usageManager = usageManager;
        this.abilityKey = abilityKey;
    }

    @Override
    public boolean canPay(Player player) {
        return !usageManager.hasUsed(player, abilityKey)
                && !cooldownManager.isOnCooldown(player, abilityKey);
    }

    @Override
    public void pay(Player player) {
        usageManager.markUsed(player, abilityKey);
    }

    @Override
    public String describeRemaining(Player player) {
        if (usageManager.hasUsed(player, abilityKey)) return "already used";
        return cooldownManager.getRemainingSeconds(player, abilityKey) + "s left";
    }

    @Override
    public int getRemainingSeconds(Player player) {
        if (usageManager.hasUsed(player, abilityKey)) return 0;
        return (int) cooldownManager.getRemainingSeconds(player, abilityKey);
    }

    @Override
    public boolean isPermanentlyUnavailable(Player player) {
        return usageManager.hasUsed(player, abilityKey);
    }
}