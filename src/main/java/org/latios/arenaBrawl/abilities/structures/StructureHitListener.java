package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StructureHitListener implements Listener {

    private static final long HIT_COOLDOWN_MILLIS = 500;
    private static final double RAY_HIT_RANGE = 5.0;

    private final StructureManager structureManager;
    private final Map<UUID, Long> lastHitAt = new HashMap<>();

    public StructureHitListener(StructureManager structureManager) {
        this.structureManager = structureManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        Action action = event.getAction();

        // Find a structure by the clicked block first. Particle-only structures (Aegis Screen)
        // have no blocks, so fall back to a ray test against their hitbox.
        PlacedStructure structure = clickedBlock != null ? findStructureByBlock(clickedBlock) : null;

        if (structure == null && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            structure = findStructureByRay(event.getPlayer());
        }

        if (structure == null) return;

        // Block right-click interaction on any structure block (totems, tables, walls...).
        if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            return;
        }

        if (action != Action.LEFT_CLICK_BLOCK && action != Action.LEFT_CLICK_AIR) return;

        event.setCancelled(true);

        Player attacker = event.getPlayer();

        long last = lastHitAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < HIT_COOLDOWN_MILLIS) return;
        lastHitAt.put(attacker.getUniqueId(), System.currentTimeMillis());

        if (!(structure instanceof DestructibleStructure destructible)) return;

        boolean canHit = destructible.canHit(attacker);
        boolean destroyed = destructible.registerHit(attacker);

        if (destroyed) {
            attacker.sendMessage("§e" + destructible.getStructureName() + " destroyed!");
            structureManager.remove(structure);
        } else if (canHit) {
            int healthPercent = destructible.getHealthPercentage();
            attacker.sendMessage(String.format("§e%s health: §6%d%%",
                    destructible.getStructureName(), healthPercent));
        }
    }

    private PlacedStructure findStructureByBlock(Block block) {
        for (PlacedStructure structure : structureManager.getAll()) {
            if (structure.getOccupiedBlocks().contains(block)) {
                return structure;
            }
        }
        return null;
    }

    private PlacedStructure findStructureByRay(Player player) {
        for (PlacedStructure structure : structureManager.getAll()) {
            if (structure instanceof DestructibleStructure destructible
                    && destructible.canHit(player)
                    && destructible.isHitByRay(player, RAY_HIT_RANGE)) {
                return structure;
            }
        }
        return null;
    }
}
