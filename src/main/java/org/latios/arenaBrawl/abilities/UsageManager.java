package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;

import java.util.*;

public class UsageManager {

    private final Map<UUID, Set<String>> usedAbilities = new HashMap<>();

    public boolean hasUsed(Player player, String abilityKey) {
        Set<String> used = usedAbilities.get(player.getUniqueId());
        return used != null && used.contains(abilityKey);
    }

    public void markUsed(Player player, String abilityKey) {
        usedAbilities
                .computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
                .add(abilityKey);
    }

    public void resetPlayer(Player player) {
        usedAbilities.remove(player.getUniqueId());
    }
}