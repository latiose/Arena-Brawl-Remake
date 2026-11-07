// abilities/structures/BarricadeStructure.java
package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BarricadeStructure extends PlacedStructure {

    private final long durationMillis;

    private final List<Block> placedBlocks = new ArrayList<>();

    public BarricadeStructure(Player owner, Location center, StructureBlueprint blueprint, long durationMillis) {
        super(owner, center.getBlock().getLocation());
        this.durationMillis = durationMillis;
        build(center.getBlock().getLocation(), blueprint);
    }

    private void build(Location base, StructureBlueprint blueprint) {
        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            Block block = base.clone().add(offset.dx(), offset.dy(), offset.dz()).getBlock();

            // No terrain restrictions: every blueprint cell is placed, even over
            // solid blocks. Blocks owned by another active structure are left alone
            // so the two structures never restore each other's stale snapshots.
            if (StructureBlockRegistry.isOwnedByOther(block, this)) continue;

            StructureBlockRegistry.claim(block, this);
            block.setType(offset.material());
            placedBlocks.add(block);
        }
    }

    public boolean isDestructibleByMelee() {
        return false;
    }

    @Override
    public boolean tick() {
        return System.currentTimeMillis() - getPlacedAt() >= durationMillis;
    }

    @Override
    public void remove() {
        for (Block block : placedBlocks) {
            StructureBlockRegistry.release(block, this);
        }
        StructureBlockRegistry.releaseAll(this);
        placedBlocks.clear();
    }

    public List<Block> getPlacedBlocks() {
        return placedBlocks;
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return placedBlocks;
    }
}