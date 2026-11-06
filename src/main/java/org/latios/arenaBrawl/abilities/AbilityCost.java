package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;

public interface AbilityCost {
    boolean canPay(Player player);
    void pay(Player player);
    String describeRemaining(Player player);

    default int getRemainingSeconds(Player player) {
        return 0;
    }

    default boolean isPermanentlyUnavailable(Player player) {
        return false;
    }

    /** True when the cost is energy-backed, so failures should not read like cooldown waits. */
    default boolean usesEnergy() {
        return false;
    }

    default String getBaseCostDescription() {
        return "";
    }
}
