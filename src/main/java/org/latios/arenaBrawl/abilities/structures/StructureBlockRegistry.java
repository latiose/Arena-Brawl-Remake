package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of which structure currently owns each block it placed.
 * <p>
 * This prevents overlapping structures (for example a Tree of Life inside a Barricade)
 * from restoring stale block snapshots. When a structure overwrites a block that already
 * belongs to another structure, it inherits the original snapshot and takes ownership,
 * so the block is eventually restored to its true original state instead of a block that
 * belonged to a structure that no longer exists.
 */
final class StructureBlockRegistry {

    private static final Map<Block, Entry> ENTRIES = new HashMap<>();

    private StructureBlockRegistry() {
    }

    private record Entry(PlacedStructure owner, BlockData original) {
    }

    /** Records the block as owned by the structure, inheriting the true original if another structure already owned it. */
    static void claim(Block block, PlacedStructure owner) {
        Entry existing = ENTRIES.get(block);
        BlockData original = existing != null ? existing.original() : block.getBlockData();
        ENTRIES.put(block, new Entry(owner, original));
    }

    /** Returns true if the block is currently owned by a different structure. */
    static boolean isOwnedByOther(Block block, PlacedStructure owner) {
        Entry entry = ENTRIES.get(block);
        return entry != null && entry.owner() != owner;
    }

    /** Restores the block to its original state only if this structure still owns it. */
    static void release(Block block, PlacedStructure owner) {
        Entry entry = ENTRIES.get(block);
        if (entry == null || entry.owner() != owner) {
            return;
        }
        block.setBlockData(entry.original());
        ENTRIES.remove(block);
    }

    /** Drops every block owned by this structure without touching the world. */
    static void releaseAll(PlacedStructure owner) {
        ENTRIES.values().removeIf(entry -> entry.owner() == owner);
    }
}
