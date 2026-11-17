package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles hunger drained by the Rune of Starvation.
 *
 * <p>Removing food points alone is useless because {@link HungerManager} regenerates
 * hunger every tick while the player is not sprinting. This tracker keeps the removed
 * hunger in place by blocking regeneration for a limited time.</p>
 */
public final class HungerSuppression {

    private static final Map<UUID, Long> regenBlockedUntil = new HashMap<>();

    private HungerSuppression() {
    }

    /**
     * Removes {@code hungerBars} bars of hunger and blocks hunger regeneration for
     * {@code noRegenMillis} so the loss actually sticks.
     */
    public static void starve(Player player, int hungerBars, long noRegenMillis) {
        if (player == null) return;

        if (hungerBars > 0) {
            int foodPoints = hungerBars * 2;
            player.setFoodLevel(Math.max(0, player.getFoodLevel() - foodPoints));
        }

        if (noRegenMillis > 0) {
            long expiresAt = System.currentTimeMillis() + noRegenMillis;
            regenBlockedUntil.merge(player.getUniqueId(), expiresAt, Math::max);
        }
    }

    public static boolean isRegenBlocked(Player player) {
        Long expiresAt = regenBlockedUntil.get(player.getUniqueId());
        if (expiresAt == null) return false;

        if (System.currentTimeMillis() > expiresAt) {
            regenBlockedUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public static void clear(Player player) {
        regenBlockedUntil.remove(player.getUniqueId());
    }
}
