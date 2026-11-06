// abilities/structures/BarricadeStructure.java
package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarricadeStructure extends PlacedStructure {

    private final long durationMillis;

    private final Map<Block, BlockData> originalBlockData = new HashMap<>();
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
            // solid blocks. The original BlockData is always saved so that
            // remove() restores whatever was there instead of deleting it.
            originalBlockData.put(block, block.getBlockData());
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
            BlockData original = originalBlockData.get(block);
            if (original != null) {
                block.setBlockData(original);
            } else {
                block.setType(Material.AIR);
            }
        }
        placedBlocks.clear();
        originalBlockData.clear();
    }

    public List<Block> getPlacedBlocks() {
        return placedBlocks;
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return placedBlocks;
    }
}