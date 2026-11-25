package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Lets allies phase through the walls of a {@link PhaseWallStructure} by mirroring their movement
 * to the other side. Enemies are stopped by the solid blocks and ignored by the structure.
 */
public class PhaseWallListener implements Listener {

    private final StructureManager structureManager;

    public PhaseWallListener(StructureManager structureManager) {
        this.structureManager = structureManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Ignore head-only movement (same block position).
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        for (PlacedStructure structure : structureManager.getAll()) {
            if (structure instanceof PhaseWallStructure wall) {
                Location arrival = wall.resolvePhase(event.getPlayer(), from, to);
                if (arrival != null) {
                    event.setTo(arrival);
                    return;
                }
            }
        }
    }
}
