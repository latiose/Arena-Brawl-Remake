
package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class PlacedStructure {

    private final UUID ownerId;
    private final Location location;
    private final long placedAt;

    protected PlacedStructure(Player owner, Location location) {
        this.ownerId = owner.getUniqueId();
        this.location = location;
        this.placedAt = System.currentTimeMillis();
    }

    public UUID getOwnerId() { return ownerId; }
    public Location getLocation() { return location; }
    public long getPlacedAt() { return placedAt; }

    /** Called every tick/interval by the manager; return true once this structure should be removed. */
    public abstract boolean tick();

    /** Called when the structure is destroyed/removed, for cleanup (entities, blocks, etc.). */
    public abstract void remove();

    public Player getOwner() {
        return Bukkit.getPlayer(ownerId);
    }
}