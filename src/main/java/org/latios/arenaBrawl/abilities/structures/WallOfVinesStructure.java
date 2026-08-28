package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WallOfVinesStructure extends PlacedStructure {

    private static final long DURATION_MILLIS = 5_000;
    private static final double IMMOBILIZE_RADIUS = 3.0;
    private static final long IMMOBILIZE_DURATION_MILLIS = 2_000;
    private final DebuffManager debuffManager;

    private final Map<Block, BlockData> originalBlockData = new HashMap<>();
    private final List<Block> placedBlocks = new ArrayList<>();
    private final Set<UUID> immobilizedEnemies = new HashSet<>();
    private final TeamManager teamManager;

    public WallOfVinesStructure(Player owner, Location center, StructureBlueprint blueprint, TeamManager teamManager, DebuffManager debuffManager) {
        super(owner, center.getBlock().getLocation());
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        build(center.getBlock().getLocation(), getPlayerFacing(owner), blueprint);
    }

    private void build(Location base, BlockFace facing, StructureBlueprint blueprint) {
        List<StructureBlueprint.BlockOffset> vineOffsets = new ArrayList<>();

        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            if (offset.material() == Material.VINE) {
                vineOffsets.add(offset);
                continue;
            }

            int[] rotated = rotateOffset(offset.dx(), offset.dz(), facing);
            Block block = base.clone().add(rotated[0], offset.dy(), rotated[1]).getBlock();

            if (!isReplaceable(block.getType())) {
                continue;
            }

            originalBlockData.put(block, block.getBlockData());
            block.setType(offset.material());
            placedBlocks.add(block);
        }

        for (StructureBlueprint.BlockOffset offset : vineOffsets) {
            int[] rotated = rotateOffset(offset.dx(), offset.dz(), facing);
            Block block = base.clone().add(rotated[0], offset.dy(), rotated[1]).getBlock();

            if (!isReplaceable(block.getType())) {
                continue;
            }

            originalBlockData.put(block, block.getBlockData());
            placeVine(block);
            placedBlocks.add(block);
        }
    }

    private void placeVine(Block block) {
        block.setType(Material.VINE, false);
        BlockData data = block.getBlockData();

        if (data instanceof MultipleFacing vineData) {
            BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            boolean attached = false;

            for (BlockFace face : faces) {
                if (block.getRelative(face).getType().isSolid()) {
                    vineData.setFace(face, true);
                    attached = true;
                }
            }

            if (!attached && block.getRelative(BlockFace.UP).getType().isSolid()) {
                vineData.setFace(BlockFace.UP, true);
                attached = true;
            }

            if (attached) {
                block.setBlockData(vineData, false);
            } else {
                vineData.setFace(BlockFace.SOUTH, true);
                block.setBlockData(vineData, false);
            }
        }
    }

    public static BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        yaw %= 360;

        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315) return BlockFace.EAST;
        return BlockFace.SOUTH;
    }

    private int[] rotateOffset(int dx, int dz, BlockFace facing) {
        return switch (facing) {
            case SOUTH -> new int[]{ dz,  dx};
            case NORTH -> new int[]{-dz, -dx};
            case EAST  -> new int[]{ dx, -dz};
            case WEST  -> new int[]{-dx,  dz};
            default    -> new int[]{dx, dz};
        };
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
        Player owner = org.bukkit.Bukkit.getPlayer(getOwnerId());
        Location center = getLocation().clone().add(0.5, 0.5, 0.5);

        for (Block block : placedBlocks) {
            if (block.getY() == getLocation().getBlockY()) {
                block.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        block.getLocation().add(0.5, 0.2, 0.5),
                        2, 0.3, 0.1, 0.3, 0.02
                );
            }
        }

        if (owner != null && owner.isOnline()) {
            for (Entity entity : center.getWorld().getNearbyEntities(center, IMMOBILIZE_RADIUS, IMMOBILIZE_RADIUS, IMMOBILIZE_RADIUS)) {
                if (entity instanceof Player target && teamManager.isEnemy(owner, target)) {
                    if (!immobilizedEnemies.contains(target.getUniqueId())) {
                        immobilizePlayer(target);
                        immobilizedEnemies.add(target.getUniqueId());
                    }
                }
            }
        }

        return System.currentTimeMillis() - getPlacedAt() >= DURATION_MILLIS;
    }

    private void immobilizePlayer(Player target) {
        debuffManager.tryApply(target, DebuffType.IMMOBILIZE, IMMOBILIZE_DURATION_MILLIS);
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