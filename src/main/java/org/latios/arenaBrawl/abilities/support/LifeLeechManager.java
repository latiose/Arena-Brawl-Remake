
package org.latios.arenaBrawl.abilities.support;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LifeLeechManager {

    private record ActiveLeech(int remainingHits, long expiresAt) {}

    private final Map<UUID, ActiveLeech> active = new HashMap<>();

    public void activate(Player player, int hitCount, long durationMillis) {
        active.put(player.getUniqueId(), new ActiveLeech(hitCount, System.currentTimeMillis() + durationMillis));
    }

    public boolean isActive(Player player) {
        ActiveLeech leech = active.get(player.getUniqueId());
        if (leech == null) return false;

        if (System.currentTimeMillis() > leech.expiresAt() || leech.remainingHits() <= 0) {
            active.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    /**
     * Consumes one charge if the player has an active Life Leech. Returns true if a charge
     * was consumed (caller should apply the heal). Returns false if inactive/expired/out of charges.
     */
    public boolean consumeCharge(Player player) {
        ActiveLeech leech = active.get(player.getUniqueId());
        if (leech == null) return false;

        if (System.currentTimeMillis() > leech.expiresAt()) {
            active.remove(player.getUniqueId());
            return false;
        }

        int remaining = leech.remainingHits() - 1;
        if (remaining <= 0) {
            active.remove(player.getUniqueId());
        } else {
            active.put(player.getUniqueId(), new ActiveLeech(remaining, leech.expiresAt()));
        }

        return true;
    }

    public int getRemainingHits(Player player) {
        ActiveLeech leech = active.get(player.getUniqueId());
        return leech != null ? leech.remainingHits() : 0;
    }

    public void clear(Player player) {
        active.remove(player.getUniqueId());
    }
}