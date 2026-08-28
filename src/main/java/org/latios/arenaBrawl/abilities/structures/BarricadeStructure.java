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

    private static final long DURATION_MILLIS = 4_000;

    private final Map<Block, BlockData> originalBlockData = new HashMap<>();
    private final List<Block> placedBlocks = new ArrayList<>();

    public BarricadeStructure(Player owner, Location center, StructureBlueprint blueprint) {
        super(owner, center.getBlock().getLocation());
        build(center.getBlock().getLocation(), blueprint);
    }


    public static boolean canBuild(Location center, StructureBlueprint blueprint) {
        Location base = center.getBlock().getLocation();
        List<StructureBlueprint.BlockOffset> offsets = blueprint.getOffsets();

        if (offsets.isEmpty()) return false;

        int minY = Integer.MAX_VALUE;
        for (StructureBlueprint.BlockOffset offset : offsets) {
            if (offset.dy() < minY) {
                minY = offset.dy();
            }
        }

        for (StructureBlueprint.BlockOffset offset : offsets) {
            Block block = base.clone().add(offset.dx(), offset.dy(), offset.dz()).getBlock();

            if (!isReplaceable(block.getType())) {
                return false;
            }

            if (offset.dy() == minY) {
                Block groundBlock = base.clone().add(offset.dx(), offset.dy() - 1, offset.dz()).getBlock();
                if (isReplaceable(groundBlock.getType()) || !groundBlock.getType().isSolid()) {
                    return false;
                }
            }
        }

        return true;
    }

    private void build(Location base, StructureBlueprint blueprint) {
        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            Block block = base.clone().add(offset.dx(), offset.dy(), offset.dz()).getBlock();

            if (!isReplaceable(block.getType())) {
                continue;
            }

            originalBlockData.put(block, block.getBlockData());
            block.setType(offset.material());
            placedBlocks.add(block);
        }
    }

    public static boolean isReplaceable(Material material) {
        return material.isAir()
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.SNOW
                || material.name().endsWith("_CARPET");
    }

    public boolean isDestructibleByMelee() {
        return false;
    }

    @Override
    public boolean tick() {
        return System.currentTimeMillis() - getPlacedAt() >= DURATION_MILLIS;
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
}