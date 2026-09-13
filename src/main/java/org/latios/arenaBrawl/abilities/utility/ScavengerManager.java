package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScavengerManager {

    private final Map<UUID, Long> activeUntil = new HashMap<>();

    public void activate(Player player, long durationMillis) {
        activeUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    public boolean isActive(Player player) {
        Long expiresAt = activeUntil.get(player.getUniqueId());
        if (expiresAt == null) return false;

        if (System.currentTimeMillis() > expiresAt) {
            activeUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void clear(Player player) {
        activeUntil.remove(player.getUniqueId());
    }
}