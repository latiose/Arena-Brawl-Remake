package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks players who cannot lose hunger for a limited time (for example the
 * Rune of Hunger). State is per-player and expires automatically.
 */
public final class HungerImmunity {

    private static final Map<UUID, Long> activeUntil = new HashMap<>();

    private HungerImmunity() {
    }

    public static void apply(Player player, long durationMillis) {
        if (durationMillis <= 0) return;
        long expiresAt = System.currentTimeMillis() + durationMillis;
        activeUntil.merge(player.getUniqueId(), expiresAt, Math::max);
    }

    public static boolean isActive(Player player) {
        Long expiresAt = activeUntil.get(player.getUniqueId());
        if (expiresAt == null) return false;

        if (System.currentTimeMillis() > expiresAt) {
            activeUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public static void clear(Player player) {
        activeUntil.remove(player.getUniqueId());
    }
}
