package org.latios.arenaBrawl.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.latios.arenaBrawl.game.MatchType;
import org.latios.arenaBrawl.queue.QueueManager;

public class QueueGUIListener implements Listener {
    private final QueueManager queueManager;
    private final QueueGUI gui;

    public QueueGUIListener(QueueManager queueManager, QueueGUI gui) {
        this.queueManager = queueManager;
        this.gui = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QueueGUI.QueueHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        MatchType[] modes = MatchType.values();
        for (int i = 0; i < modes.length; i++) {
            if (slot != 11 + i * 2) continue;
            MatchType mode = modes[i];
            if (queueManager.isQueued(player, mode)) {
                queueManager.leaveQueue(player, mode);
                player.sendMessage("§eYou left the " + mode.getDisplayName() + " queue.");
            } else {
                int size = queueManager.joinQueue(player, mode);
                if (size < 0) {
                    player.sendMessage("§cA member of your party is already in a queue.");
                } else {
                    player.sendMessage("§aYou joined " + mode.getDisplayName() + " (" + size + "/" + mode.playersNeeded() + ").");
                }
            }
            player.closeInventory();
            return;
        }
    }
}
