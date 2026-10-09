package org.latios.arenaBrawl.debuffs;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.general.MovementLockManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ImmobilizeListener implements Listener {

    private static final double NUDGE_SPEED = 0.32;
    private static final double NUDGE_MAX_DISTANCE = 0.6;
    private static final long NUDGE_WINDOW_MS = 300;
    private static final long NUDGE_COOLDOWN_MS = 350;
    private static final double FALL_SPEED = -1.5;
    private record Nudge(long startMs, double x, double z) {}

    private final DebuffManager debuffManager;
    private final MovementLockManager movementLockManager;
    private final Map<UUID, Nudge> nudges = new HashMap<>();

    public ImmobilizeListener(DebuffManager debuffManager, MovementLockManager movementLockManager) {
        this.debuffManager = debuffManager;
        this.movementLockManager = movementLockManager;
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!isCurrentlyImmobilizing(player)) return;
        event.setCancelled(true);

        if (movementLockManager.isLocked(player)) return;

        long now = System.currentTimeMillis();
        Nudge last = nudges.get(player.getUniqueId());
        if (last != null && now - last.startMs() < NUDGE_COOLDOWN_MS) return;

        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-6) return;
        dir.normalize().multiply(NUDGE_SPEED);

        Location origin = event.getFrom();
        nudges.put(player.getUniqueId(), new Nudge(now, origin.getX(), origin.getZ()));
        player.setVelocity(dir);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isCurrentlyImmobilizing(player)) return;
        if (movementLockManager.isLocked(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        boolean movedHorizontally = dx != 0 || dz != 0;
        boolean rising = dy > 0;

        Nudge nudge = activeNudge(player);

        if (nudge == null && isAirborne(player)) {
            player.setVelocity(new Vector(0, FALL_SPEED, 0));

            if (rising) {
                Location adjusted = to.clone();
                adjusted.setX(from.getX());
                adjusted.setY(from.getY());
                adjusted.setZ(from.getZ());
                event.setTo(adjusted);
            }
            return;
        }

        Location adjusted = to.clone();
        boolean changed = false;

        if (nudge != null) {
            double ox = to.getX() - nudge.x();
            double oz = to.getZ() - nudge.z();
            double dist = Math.hypot(ox, oz);
            if (dist > NUDGE_MAX_DISTANCE) {
                double scale = NUDGE_MAX_DISTANCE / dist;
                adjusted.setX(nudge.x() + ox * scale);
                adjusted.setZ(nudge.z() + oz * scale);
                changed = true;
            }
        } else if (movedHorizontally) {
            adjusted.setX(from.getX());
            adjusted.setZ(from.getZ());
            changed = true;
        }

        if (rising) {
            adjusted.setY(from.getY());
            Vector velocity = player.getVelocity();
            if (velocity.getY() > 0) {
                player.setVelocity(velocity.setY(0));
            }
            changed = true;
        }

        if (changed) event.setTo(adjusted);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        nudges.remove(event.getPlayer().getUniqueId());
    }

    private Nudge activeNudge(Player player) {
        Nudge nudge = nudges.get(player.getUniqueId());
        if (nudge == null) return null;
        if (System.currentTimeMillis() - nudge.startMs() > NUDGE_WINDOW_MS) {
            nudges.remove(player.getUniqueId());
            return null;
        }
        return nudge;
    }

    private boolean isAirborne(Player player) {
        if (player.isOnGround()) return false;
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        if (player.isFlying() || player.isGliding()) return false;
        if (player.isInWater() || player.isSwimming() || player.isClimbing()) return false;
        return true;
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