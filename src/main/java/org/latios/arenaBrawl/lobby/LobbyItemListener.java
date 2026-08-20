package org.latios.arenaBrawl.lobby;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.latios.arenaBrawl.gui.AbilitySelectorGUI;
import org.latios.arenaBrawl.queue.QueueManager;

public class LobbyItemListener implements Listener {

    private final QueueManager queueManager;
    private final AbilitySelectorGUI abilitySelectorGUI;

    public LobbyItemListener(QueueManager queueManager, AbilitySelectorGUI abilitySelectorGUI) {
        this.queueManager = queueManager;
        this.abilitySelectorGUI = abilitySelectorGUI;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        Player player = event.getPlayer();

        if (item.getType() == Material.COMPASS) {
            event.setCancelled(true);
            toggleQueue(player);
        } else if (item.getType() == Material.EMERALD) {
            event.setCancelled(true);
            abilitySelectorGUI.openMainMenu(player);
        }
    }

    private void toggleQueue(Player player) {
        if (queueManager.isQueued(player)) {
            queueManager.leaveQueue(player);
            player.sendMessage("§eYou left the queue.");
        } else {
            boolean joined = queueManager.joinQueue(player);
            if (joined) {
                player.sendMessage("§aYou joined the queue (" + queueManager.getQueueSize() + "/4).");
            } else {
                player.sendMessage("§cA member of your party is already in queue.");
            }
        }
    }
}