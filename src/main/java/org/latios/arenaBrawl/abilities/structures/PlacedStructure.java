
package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
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

    public Player getOwner() {
        return org.bukkit.Bukkit.getPlayer(ownerId);
    }

    public abstract boolean tick();
    public abstract void remove();

    /** All physical blocks belonging to this structure, used for hit-detection by abilities like Bull Charge/GolemFall. */
    public abstract List<Block> getOccupiedBlocks();
}