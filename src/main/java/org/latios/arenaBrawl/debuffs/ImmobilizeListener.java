
package org.latios.arenaBrawl.debuffs;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class ImmobilizeListener implements Listener {

    private final DebuffManager debuffManager;

    public ImmobilizeListener(DebuffManager debuffManager) {
        this.debuffManager = debuffManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isCurrentlyImmobilizing(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        boolean lockedX = from.getX() != to.getX();
        boolean lockedZ = from.getZ() != to.getZ();
        boolean jumping = to.getY() > from.getY(); // moving upward = jump attempt

        if (!lockedX && !lockedZ && !jumping) return; // nothing to block

        Location adjusted = to.clone();
        if (lockedX) adjusted.setX(from.getX());
        if (lockedZ) adjusted.setZ(from.getZ());
        if (jumping) adjusted.setY(from.getY()); // block upward movement, allow falling

        event.setTo(adjusted);

        if (jumping) {
            // Zero out any upward velocity so the jump doesn't "pop" once cancelled
            Vector velocity = player.getVelocity();
            if (velocity.getY() > 0) {
                player.setVelocity(velocity.setY(0));
            }
        }
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