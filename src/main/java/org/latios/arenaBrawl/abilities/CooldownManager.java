package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(Player player, String abilityKey) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;
        Long readyAt = playerCooldowns.get(abilityKey);
        return readyAt != null && System.currentTimeMillis() < readyAt;
    }

    public long getRemainingSeconds(Player player, String abilityKey) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        Long readyAt = playerCooldowns.get(abilityKey);
        if (readyAt == null) return 0;
        return Math.max(0, (readyAt - System.currentTimeMillis()) / 1000);
    }

    public void setCooldown(Player player, String abilityKey, long cooldownMillis) {
        cooldowns
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(abilityKey, System.currentTimeMillis() + cooldownMillis);
    }
}
