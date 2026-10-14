package org.latios.arenaBrawl.debuffs;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.general.MovementLockManager;

public class ImmobilizeListener implements Listener {

    private static final double FALL_SPEED = -2.0;
    private final DebuffManager debuffManager;
    private final MovementLockManager movementLockManager;

    public ImmobilizeListener(DebuffManager debuffManager, MovementLockManager movementLockManager) {
        this.debuffManager = debuffManager;
        this.movementLockManager = movementLockManager;
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!isCurrentlyImmobilizing(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        boolean immobilized = isCurrentlyImmobilizing(player) && !movementLockManager.isLocked(player);

        if (!immobilized) {
            removeRoot(player);
            return;
        }
        applyRoot(player);

        Location from = event.getFrom();
        Location to = event.getTo();

        if (isAirborne(player)) {
            player.setVelocity(new Vector(0, FALL_SPEED, 0));
        }

        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            Location fixed = to.clone();
            fixed.setX(from.getX());
            fixed.setZ(from.getZ());
            event.setTo(fixed);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeRoot(event.getPlayer());
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

    private static final NamespacedKey NO_JUMP_KEY = new NamespacedKey("arenabrawl", "immobilize_nojump");

    private void applyRoot(Player player) {
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null && jump.getModifier(NO_JUMP_KEY) == null) {
            jump.addModifier(new AttributeModifier(NO_JUMP_KEY, -1.0,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    private void removeRoot(Player player) {
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump != null) jump.removeModifier(NO_JUMP_KEY);
    }
}