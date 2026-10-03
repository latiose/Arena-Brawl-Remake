
package org.latios.arenaBrawl.hats;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import org.latios.arenaBrawl.stats.StatsManager;

import java.util.List;

public class MagicChestGUI {
    public static final int SLOT_BUY = 3;
    public static final int SLOT_OPEN = 5;
    public static final int SLOT_MODE = 8;
    public record MagicChestHolder() implements InventoryHolder {
        @Override
        public @NonNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }

    private final KeyManager keyManager;
    private final StatsManager statsManager;

    private final ClickModeManager clickMode;

    public MagicChestGUI(KeyManager keyManager, StatsManager statsManager, ClickModeManager clickMode) {
        this.keyManager = keyManager;
        this.statsManager = statsManager;
        this.clickMode = clickMode;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new MagicChestHolder(), 9, "§5§lMagic Chest");
        refresh(inv, player);
        player.openInventory(inv);
    }

    public void refresh(Inventory inv, Player player) {
        int coins = statsManager.getStats(player).coins;
        int keys = keyManager.getKeys(player);

        ItemStack buyKeyItem = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta buyMeta = buyKeyItem.getItemMeta();
        buyMeta.setDisplayName("§eBuy a Key §7(" + KeyManager.getKeyCost() + " coins)");
        buyMeta.setLore(List.of("§7Your coins: §6" + coins));
        buyKeyItem.setItemMeta(buyMeta);
        inv.setItem(SLOT_BUY, buyKeyItem);

        ItemStack openChestItem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta openMeta = openChestItem.getItemMeta();
        openMeta.setDisplayName("§dOpen the Chest");
        openMeta.setLore(List.of("§7Your keys: §b" + keys));
        openChestItem.setItemMeta(openMeta);
        inv.setItem(SLOT_OPEN, openChestItem);
        boolean precise = clickMode.isPrecise(player);
        ItemStack modeItem = new ItemStack(Material.COMPARATOR);
        ItemMeta modeMeta = modeItem.getItemMeta();
        modeMeta.setDisplayName("§6Click mode: " + (precise ? "§aPrecise" : "§eFast"));
        modeMeta.setLore(precise
                ? List.of("§7Only real single clicks count.",
                "§7Ignores double clicks, shift-clicks",
                "§7and rapid repeats.", "", "§eClick to switch to Fast")
                : List.of("§7Every click event is counted.", "", "§eClick to switch to Precise"));
        modeItem.setItemMeta(modeMeta);
        inv.setItem(SLOT_MODE, modeItem);
    }
}