package org.latios.arenaBrawl.hats;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClickModeManager {

    private static final long COOLDOWN_MS = 250;

    private final Set<UUID> precise = new HashSet<>();
    private final Map<UUID, Long> lastAccepted = new HashMap<>();

    public boolean isPrecise(Player player) {
        return precise.contains(player.getUniqueId());
    }

    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (precise.remove(id)) return false;
        precise.add(id);
        return true;
    }

    public boolean passesCooldown(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastAccepted.get(id);
        if (last != null && now - last < COOLDOWN_MS) return false;
        lastAccepted.put(id, now);
        return true;
    }

    public void remove(UUID id) {
        precise.remove(id);
        lastAccepted.remove(id);
    }
}