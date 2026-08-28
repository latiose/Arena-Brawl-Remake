package org.latios.arenaBrawl.abilities.utility;

import me.libraryaddict.disguise.DisguiseAPI;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.structures.BarricadeStructure;
import org.latios.arenaBrawl.abilities.structures.HealingTotemStructure;
import org.latios.arenaBrawl.abilities.structures.PlacedStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.structures.WallOfVinesStructure;
import org.latios.arenaBrawl.general.MovementLockManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

public class BullChargeTask extends BukkitRunnable {

    private static final double TOTAL_DISTANCE = 15.0;
    private static final long DURATION_TICKS = 20;
    private static final double STEP_DISTANCE = TOTAL_DISTANCE / DURATION_TICKS;
    private static final double STRUCTURE_HIT_RADIUS = 1.2;

    private final Player player;
    private final Vector direction;
    private final StructureManager structureManager;
    private final MovementLockManager movementLockManager;
    private final TeamManager teamManager;

    private double traveled = 0.0;
    private int ticksElapsed = 0;

    public BullChargeTask(Player player, Vector direction, StructureManager structureManager,
                          MovementLockManager movementLockManager, TeamManager teamManager) {
        this.player = player;
        this.direction = direction;
        this.structureManager = structureManager;
        this.movementLockManager = movementLockManager;
        this.teamManager = teamManager;
    }

    @Override
    public void run() {
        if (!player.isOnline() || ticksElapsed >= DURATION_TICKS || traveled >= TOTAL_DISTANCE) {
            end();
            return;
        }

        Location current = player.getLocation();
        Location next = current.clone().add(direction.clone().multiply(STEP_DISTANCE));

        PlacedStructure hitStructure = findStructureNear(next);
        if (hitStructure != null) {
            Player owner = hitStructure.getOwner();

            if (owner != null && teamManager.isEnemy(player, owner)) {
                structureManager.remove(hitStructure);
                next.getWorld().spawnParticle(Particle.EXPLOSION, next, 2);
                next.getWorld().playSound(next, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                player.teleport(next);
                end();
                return;
            } else if (owner != null && !teamManager.isEnemy(player, owner)) {
                end();
                return;
            }
        }

        if (isBlockedByTerrain(next)) {
            end();
            return;
        }

        player.teleport(next);
        current.getWorld().spawnParticle(Particle.CLOUD, current, 3, 0.2, 0.1, 0.2, 0.01);

        traveled += STEP_DISTANCE;
        ticksElapsed++;
    }

    private PlacedStructure findStructureNear(Location point) {
        for (PlacedStructure structure : new ArrayList<>(structureManager.getAll())) {
            List<Block> blocks = getStructureBlocks(structure);
            for (Block block : blocks) {
                Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
                if (blockCenter.distance(point) <= STRUCTURE_HIT_RADIUS) {
                    return structure;
                }
            }
        }
        return null;
    }

    private List<Block> getStructureBlocks(PlacedStructure structure) {
        if (structure instanceof BarricadeStructure barricade) {
            return barricade.getPlacedBlocks();
        }
        if (structure instanceof HealingTotemStructure totem) {
            return totem.getStandBlocks();
        }
        if (structure instanceof WallOfVinesStructure wallOfVines) {
            return wallOfVines.getPlacedBlocks();
        }
        return List.of();
    }

    private boolean isBlockedByTerrain(Location destination) {
        Block block = destination.getBlock();
        Block above = destination.clone().add(0, 1, 0).getBlock();

        boolean blockSolid = block.getType().isSolid() && !isStructureBlock(block);
        boolean aboveSolid = above.getType().isSolid() && !isStructureBlock(above);

        return blockSolid || aboveSolid;
    }

    private boolean isStructureBlock(Block block) {
        for (PlacedStructure structure : structureManager.getAll()) {
            if (getStructureBlocks(structure).contains(block)) {
                return true;
            }
        }
        return false;
    }

    private void end() {
        cancel();
        if (player.isOnline()) {
            if (DisguiseAPI.isDisguised(player)) {
                DisguiseAPI.undisguiseToAll(player);
            }
            movementLockManager.unlock(player);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_COW_HURT, 1f, 0.8f);
        }
    }
}