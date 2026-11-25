package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.List;

public class StructureUtils {

    public static boolean canPlaceStructure(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSpaceClearForBlueprint(Location origin, StructureBlueprint blueprint) {
        return isSpaceClearForBlueprint(origin, BlockFace.SOUTH, blueprint);
    }

    public static boolean isSpaceClearForBlueprint(Location origin, BlockFace facing, StructureBlueprint blueprint) {
        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            int[] rotated = rotateOffset(offset.dx(), offset.dz(), facing);
            Block target = origin.clone().add(rotated[0], offset.dy(), rotated[1]).getBlock();
            if (target.getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSpaceClearForCage(Location origin, StructureBlueprint blueprint) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int maxY = 0;

        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            minX = Math.min(minX, offset.dx());
            maxX = Math.max(maxX, offset.dx());
            minZ = Math.min(minZ, offset.dz());
            maxZ = Math.max(maxZ, offset.dz());
            maxY = Math.max(maxY, offset.dy());
        }

        for (int dx = minX; dx <= maxX; dx++) {
            for (int dz = minZ; dz <= maxZ; dz++) {
                if (!origin.clone().add(dx, -1, dz).getBlock().getType().isSolid()) {
                    return false;
                }

                for (int dy = 0; dy <= maxY; dy++) {
                    Block target = origin.clone().add(dx, dy, dz).getBlock();
                    if (target.getType().isSolid() || target.isLiquid()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static int[] rotateOffset(int dx, int dz, BlockFace facing) {
        return switch (facing) {
            case SOUTH -> new int[]{ dz,  dx};
            case NORTH -> new int[]{-dz, -dx};
            case EAST  -> new int[]{ dx, -dz};
            case WEST  -> new int[]{-dx,  dz};
            default    -> new int[]{dx, dz};
        };
    }
}