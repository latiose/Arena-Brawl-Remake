package org.latios.arenaBrawl.general;


import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MovementLockManager {

    private final Set<UUID> lockedByAbility = new HashSet<>();

    public void lock(Player player) {
        lockedByAbility.add(player.getUniqueId());
    }

    public void unlock(Player player) {
        lockedByAbility.remove(player.getUniqueId());
    }

    public boolean isLocked(Player player) {
        return lockedByAbility.contains(player.getUniqueId());
    }
}