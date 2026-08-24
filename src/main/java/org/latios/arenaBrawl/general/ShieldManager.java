package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShieldManager {

    // Player -> timestamp (millis) when their active shield expires
    private final Map<UUID, Long> activeUntil = new HashMap<>();
    // Player -> reduction percentage of their current shield
    private final Map<UUID, Double> activeReduction = new HashMap<>();

    public void applyShield(Player player, double reductionPercent, long durationMillis) {
        activeUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
        activeReduction.put(player.getUniqueId(), reductionPercent);
    }

    public double getDamageReduction(Player player) {
        Long expiresAt = activeUntil.get(player.getUniqueId());
        if (expiresAt == null) return 0.0;

        if (System.currentTimeMillis() > expiresAt) {
            activeUntil.remove(player.getUniqueId());
            activeReduction.remove(player.getUniqueId());
            return 0.0;
        }

        return activeReduction.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void clear(Player player) {
        activeUntil.remove(player.getUniqueId());
        activeReduction.remove(player.getUniqueId());
    }

    public boolean hasShield(Player player) {
        Long expiresAt = activeUntil.get(player.getUniqueId());
        return System.currentTimeMillis() < expiresAt;
    }
}