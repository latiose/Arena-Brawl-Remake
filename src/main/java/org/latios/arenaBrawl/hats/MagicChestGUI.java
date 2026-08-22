// hats/MagicChestGUI.java
package org.latios.arenaBrawl.hats;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.latios.arenaBrawl.stats.StatsManager;

import java.util.List;

public class MagicChestGUI {

    public record MagicChestHolder() implements InventoryHolder {
        @Override
        public Inventory getInventory() { throw new UnsupportedOperationException(); }
    }

    private final KeyManager keyManager;
    private final StatsManager statsManager;

    public MagicChestGUI(KeyManager keyManager, StatsManager statsManager) {
        this.keyManager = keyManager;
        this.statsManager = statsManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new MagicChestHolder(), 9, "§5§lMagic Chest");

        int coins = statsManager.getStats(player).coins;
        int keys = keyManager.getKeys(player);

        ItemStack buyKeyItem = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta buyMeta = buyKeyItem.getItemMeta();
        buyMeta.setDisplayName("§eBuy a Key §7(" + KeyManager.getKeyCost() + " coins)");
        buyMeta.setLore(List.of("§7Your coins: §6" + coins));
        buyKeyItem.setItemMeta(buyMeta);
        inv.setItem(3, buyKeyItem);

        ItemStack openChestItem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta openMeta = openChestItem.getItemMeta();
        openMeta.setDisplayName("§dOpen the Chest");
        openMeta.setLore(List.of("§7Your keys: §b" + keys));
        openChestItem.setItemMeta(openMeta);
        inv.setItem(5, openChestItem);

        player.openInventory(inv);
    }
}