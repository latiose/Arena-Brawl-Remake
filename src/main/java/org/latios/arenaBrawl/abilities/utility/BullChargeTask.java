
package org.latios.arenaBrawl.abilities.utility;

import me.libraryaddict.disguise.DisguiseAPI;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.structures.PlacedStructure;
import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.general.MovementLockManager;

public class BullChargeTask extends BukkitRunnable {

    private static final double TOTAL_DISTANCE = 15.0;
    private static final long DURATION_TICKS = 20;
    private static final double STEP_DISTANCE = TOTAL_DISTANCE / DURATION_TICKS;
    private static final double STRUCTURE_HIT_RADIUS = 1.2;

    private final Player player;
    private final Vector direction;
    private final StructureDemolitionService demolitionService;
    private final MovementLockManager movementLockManager;

    private double traveled = 0.0;
    private int ticksElapsed = 0;

    public BullChargeTask(Player player, Vector direction, StructureDemolitionService demolitionService,
                          MovementLockManager movementLockManager) {
        this.player = player;
        this.direction = direction;
        this.demolitionService = demolitionService;
        this.movementLockManager = movementLockManager;
    }

    @Override
    public void run() {
        if (!player.isOnline() || ticksElapsed >= DURATION_TICKS || traveled >= TOTAL_DISTANCE) {
            end();
            return;
        }

        Location current = player.getLocation();
        Location next = current.clone().add(direction.clone().multiply(STEP_DISTANCE));

        PlacedStructure hitStructure = demolitionService.findStructureNear(next, STRUCTURE_HIT_RADIUS);
        if (hitStructure != null) {
            if (demolitionService.isEnemyStructure(player, hitStructure)) {
                demolitionService.demolish(hitStructure, next);
                player.teleport(next);
            }
            end(); // charge stops either way: breaks enemy structure, or blocked by an ally's
            return;
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

    private boolean isBlockedByTerrain(Location destination) {
        Block block = destination.getBlock();
        Block above = destination.clone().add(0, 1, 0).getBlock();

        boolean blockSolid = block.getType().isSolid() && !demolitionService.isBlockPartOfAnyStructure(block);
        boolean aboveSolid = above.getType().isSolid() && !demolitionService.isBlockPartOfAnyStructure(above);

        return blockSolid || aboveSolid;
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