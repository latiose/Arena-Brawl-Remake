package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds timed energy discounts applied to offensive skill costs (see Skill Manipulation).
 * {@link EnergyCost} consults this when charging energy, so every offensive ability
 * that uses an energy cost is affected while a discount is active.
 */
public final class OffensiveEnergyDiscount {

    private record Discount(double percent, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    private static final Map<UUID, Discount> activeDiscounts = new HashMap<>();

    private OffensiveEnergyDiscount() {
    }

    /** Activates (or replaces) an energy discount for the player. {@code discountPercent} is 0..1. */
    public static void activate(Player player, double discountPercent, long durationMillis) {
        activeDiscounts.put(player.getUniqueId(),
                new Discount(Math.max(0.0, discountPercent), System.currentTimeMillis() + durationMillis));
    }

    /** Multiplier applied to energy costs for this player (1.0 when no discount is active). */
    public static double getMultiplier(Player player) {
        Discount discount = activeDiscounts.get(player.getUniqueId());
        if (discount == null) return 1.0;

        if (discount.isExpired()) {
            activeDiscounts.remove(player.getUniqueId());
            return 1.0;
        }

        return 1.0 - discount.percent();
    }

    public static boolean isActive(Player player) {
        return getMultiplier(player) < 1.0;
    }

    public static void clear(Player player) {
        activeDiscounts.remove(player.getUniqueId());
    }
}
