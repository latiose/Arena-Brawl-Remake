package org.latios.arenaBrawl.lobby;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.gui.AbilitySelectorGUI;
import org.latios.arenaBrawl.gui.QueueGUI;
import org.latios.arenaBrawl.queue.QueueManager;

public class LobbyItemListener implements Listener {

    private final QueueManager queueManager;
    private final AbilitySelectorGUI abilitySelectorGUI;
    private final MatchManager matchManager;
    private final QueueGUI queueGUI;

    public LobbyItemListener(QueueManager queueManager, AbilitySelectorGUI abilitySelectorGUI, MatchManager matchManager, QueueGUI queueGUI) {
        this.queueManager = queueManager;
        this.abilitySelectorGUI = abilitySelectorGUI;
        this.matchManager = matchManager;
        this.queueGUI = queueGUI;
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
            if (matchManager.isInMatch(player)) {
                player.sendMessage("§cYou can't use the queue while in a match.");
            } else {
                queueGUI.open(player, queueManager);
            }
        } else if (item.getType() == Material.EMERALD) {
            event.setCancelled(true);

            if (matchManager.isInMatch(player)) {
                player.sendMessage("§cYou can't change abilities while in a match.");
                return;
            }

            abilitySelectorGUI.openMainMenu(player);
        }
    }

}