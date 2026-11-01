package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface ReflectStance {

    record ReflectState(long expiresAt, double reflectMultiplier, double damageReduction, String sourceName) {}

    Map<UUID, ReflectState> ACTIVE_STANCES = new ConcurrentHashMap<>();

    static void apply(Player player, ReflectState state) {
        ACTIVE_STANCES.put(player.getUniqueId(), state);
    }

    static ReflectState getActiveState(Player player) {
        UUID uuid = player.getUniqueId();
        ReflectState state = ACTIVE_STANCES.get(uuid);
        if (state == null) return null;

        if (System.currentTimeMillis() >= state.expiresAt()) {
            ACTIVE_STANCES.remove(uuid, state);
            return null;
        }
        return state;
    }

    static void clearState(Player player) {
        ACTIVE_STANCES.remove(player.getUniqueId());
    }

    default ReflectState getActive(Player player) {
        return getActiveState(player);
    }

    default void clear(Player player) {
        clearState(player);
    }
}
