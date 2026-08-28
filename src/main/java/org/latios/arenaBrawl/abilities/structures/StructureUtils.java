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