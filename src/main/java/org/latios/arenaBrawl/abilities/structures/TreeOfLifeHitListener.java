
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

public class TreeOfLifeHitListener implements Listener {

    private static final long HIT_COOLDOWN_MILLIS = 500;

    private final StructureManager structureManager;
    private final Map<UUID, Long> lastHitAt = new HashMap<>();

    public TreeOfLifeHitListener(StructureManager structureManager) {
        this.structureManager = structureManager;
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        TreeOfLifeStructure tree = findTreeByBlock(clickedBlock);
        if (tree == null) return;

        event.setCancelled(true);

        Player attacker = event.getPlayer();

        long last = lastHitAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < HIT_COOLDOWN_MILLIS) return;
        lastHitAt.put(attacker.getUniqueId(), System.currentTimeMillis());

        boolean destroyed = tree.registerHit(attacker);
        boolean canHit = tree.canHit(attacker);
        if (destroyed) {
            attacker.sendMessage("§Tree of Life destroyed!");
            structureManager.remove(tree);
        }  if (canHit) {
            int healthPercent = tree.getHealthPercentage();
            attacker.sendMessage(String.format("§eTree of life health: §6%d%%", healthPercent));
        }
    }

    private TreeOfLifeStructure findTreeByBlock(Block block) {
        for (PlacedStructure structure : structureManager.getAll()) {
            if (structure instanceof TreeOfLifeStructure tree && tree.getOccupiedBlocks().contains(block)) {
                return tree;
            }
        }
        return null;
    }
}