package org.latios.arenaBrawl.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.latios.arenaBrawl.game.MatchType;
import org.latios.arenaBrawl.queue.QueueManager;

import java.util.List;

public class QueueGUI {
    public static final int SIZE = 27;

    public void open(Player player, QueueManager queueManager) {
        Inventory inventory = Bukkit.createInventory(new QueueHolder(), SIZE, "Select Queue");
        MatchType[] modes = MatchType.values();
        for (int i = 0; i < modes.length; i++) {
            MatchType mode = modes[i];
            boolean queued = queueManager.isQueued(player, mode);
            Material material = mode == MatchType.TEAMS ? Material.BLUE_WOOL
                    : mode == MatchType.DUEL ? Material.IRON_SWORD : Material.NETHER_STAR;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((queued ? "§a✓ " : "§e") + mode.getDisplayName());
            meta.setLore(List.of("", "§7Players: §f" + mode.playersNeeded(),
                    "§7In queue: §f" + queueManager.getQueueSize(mode),
                    "", queued ? "§cClick to leave" : "§aClick to join"));
            item.setItemMeta(meta);
            inventory.setItem(11 + i * 2, item);
        }
        player.openInventory(inventory);
    }

    public static final class QueueHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
