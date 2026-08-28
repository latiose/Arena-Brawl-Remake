
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

public class HealingTotemHitListener implements Listener {

    private static final long HIT_COOLDOWN_MILLIS = 500;

    private final StructureManager structureManager;
    private final Map<UUID, Long> lastHitAt = new HashMap<>();

    public HealingTotemHitListener(StructureManager structureManager) {
        this.structureManager = structureManager;
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        HealingTotemStructure totem = findTotemByBlock(clickedBlock);
        if (totem == null) return;

        event.setCancelled(true);

        Player attacker = event.getPlayer();

        long last = lastHitAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < HIT_COOLDOWN_MILLIS) return;
        lastHitAt.put(attacker.getUniqueId(), System.currentTimeMillis());

        boolean destroyed = totem.registerHit(attacker);
        if (destroyed) {
            structureManager.remove(totem);
        }
    }

    private HealingTotemStructure findTotemByBlock(Block block) {
        for (PlacedStructure structure : structureManager.getAll()) {
            if (structure instanceof HealingTotemStructure totem && totem.getStandBlocks().contains(block)) {
                return totem;
            }
        }
        return null;
    }
}