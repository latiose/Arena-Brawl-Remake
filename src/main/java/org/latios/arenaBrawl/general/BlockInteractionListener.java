package org.latios.arenaBrawl.general;

import org.bukkit.block.Container;
import org.bukkit.block.EnchantingTable;
import org.bukkit.block.EnderChest;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockInteractionListener implements Listener {

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        if (event.getClickedBlock().getState() instanceof Sign) {
            event.setCancelled(true);
        }
    }



    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        var state = event.getClickedBlock().getState();

        if (state instanceof Sign || state instanceof Container || state  instanceof EnchantingTable) {
            event.setCancelled(true);
        }
        if(state instanceof EnderChest) {
            event.setCancelled(false);
        }
    }
}