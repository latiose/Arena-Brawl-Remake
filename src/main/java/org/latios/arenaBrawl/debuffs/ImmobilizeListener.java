package org.latios.arenaBrawl.debuffs;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.general.MovementLockManager;

public class ImmobilizeListener implements Listener {

    private static final double MAX_GROUND_CHECK = 128.0;
    private static final double[][] FOOT_OFFSETS = {
            {0, 0}, {0.3, 0.3}, {0.3, -0.3}, {-0.3, 0.3}, {-0.3, -0.3}
    };

    private final DebuffManager debuffManager;
    private final MovementLockManager movementLockManager;

    public ImmobilizeListener(DebuffManager debuffManager, MovementLockManager movementLockManager) {
        this.debuffManager = debuffManager;
        this.movementLockManager = movementLockManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isCurrentlyImmobilizing(player)) return;
        if (movementLockManager.isLocked(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (isAirborne(player)) {
            event.setTo(from.clone().set(from.getX(), to.getY(), from.getZ()));
            player.setVelocity(new Vector(0, -3, 0));
        }

        boolean lockedX = from.getX() != to.getX();
        boolean lockedZ = from.getZ() != to.getZ();
        boolean jumping = to.getY() > from.getY();

        if (!lockedX && !lockedZ && !jumping) return;

        Location adjusted = to.clone();
        if (lockedX) adjusted.setX(from.getX());
        if (lockedZ) adjusted.setZ(from.getZ());
        if (jumping) adjusted.setY(from.getY());

        event.setTo(adjusted);

        if (jumping) {
            Vector velocity = player.getVelocity();
            if (velocity.getY() > 0) {
                player.setVelocity(velocity.setY(0));
            }
        }
    }

    private boolean isAirborne(Player player) {
        if (player.isOnGround()) return false;
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        if (player.isFlying() || player.isGliding()) return false;
        if (player.isInWater() || player.isSwimming() || player.isClimbing()) return false;
        return true;
    }

    private Double findGroundY(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        Double best = null;
        for (double[] offset : FOOT_OFFSETS) {
            Location start = loc.clone().add(offset[0], 0.1, offset[1]);
            RayTraceResult result = world.rayTraceBlocks(
                    start, new Vector(0, -1, 0), MAX_GROUND_CHECK,
                    FluidCollisionMode.NEVER, true
            );
            if (result != null) {
                double y = result.getHitPosition().getY();
                if (best == null || y > best) best = y;
            }
        }
        return best;
    }

    private boolean isCurrentlyImmobilizing(Player player) {
        for (DebuffType type : DebuffType.values()) {
            if (type.isImmobilizing() && debuffManager.hasDebuff(player, type)) {
                return true;
            }
        }
        return false;
    }
}