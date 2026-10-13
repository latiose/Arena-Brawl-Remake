package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class SwordSelectorGUI {
    public static final int SIZE = 27;

    public record SwordSelectorHolder() implements InventoryHolder {
        @Override
        public @NonNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }

    private final SwordSelectionManager selectionManager;

    public SwordSelectorGUI(SwordSelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new SwordSelectorHolder(), SIZE, "§bSword Selector");
        SwordType[] swords = SwordType.values();
        SwordType equipped = selectionManager.getEquipped(player);
        for (int i = 0; i < swords.length; i++) {
            SwordType sword = swords[i];
            boolean unlocked = selectionManager.isUnlocked(player, sword);
            ItemStack item = new ItemStack(unlocked ? sword.getMaterial() : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(unlocked
                    ? (sword == equipped ? "§a✔ " : "§f") + sword.getDisplayName()
                    : "§7??? Sword");
            meta.setLore(unlocked
                    ? List.of("", sword == equipped ? "§aCurrently equipped" : "§eClick to equip")
                    : List.of("", "§7Not unlocked yet.", "§7Find it in the §dMagic Chest§7!"));
            item.setItemMeta(meta);
            inventory.setItem(10 + i, item);
        }
        player.openInventory(inventory);
    }
}
