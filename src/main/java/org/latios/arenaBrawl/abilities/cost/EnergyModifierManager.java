package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnergyModifierManager {

    private final Map<UUID, Map<String, ModifierEntry>> activeModifiers = new HashMap<>();

    private static class ModifierEntry {
        final double multiplier;
        final long expiresAt;

        ModifierEntry(double multiplier, long durationMillis) {
            this.multiplier = multiplier;
            this.expiresAt = System.currentTimeMillis() + durationMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public void addModifier(Player player, String id, double multiplier, long durationMillis) {
        activeModifiers
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(id, new ModifierEntry(multiplier, durationMillis));
    }

    public double getTotalMultiplier(Player player) {
        Map<String, ModifierEntry> map = activeModifiers.get(player.getUniqueId());
        if (map == null || map.isEmpty()) return 1.0;

        double totalMultiplier = 1.0;

        var iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ModifierEntry mod = entry.getValue();

            if (mod.isExpired()) {
                iterator.remove();
            } else {
                totalMultiplier *= mod.multiplier;
            }
        }

        return totalMultiplier;
    }

    public void clear(Player player) {
        activeModifiers.remove(player.getUniqueId());
    }
}