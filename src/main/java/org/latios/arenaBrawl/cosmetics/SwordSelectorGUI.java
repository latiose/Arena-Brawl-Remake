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
    public static final int RANDOM_SLOT = 22;

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
        boolean randomEquipped = selectionManager.isRandom(player);
        SwordType equipped = randomEquipped ? null : selectionManager.getEquipped(player);
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
        inventory.setItem(RANDOM_SLOT, buildRandomItem(randomEquipped));
        player.openInventory(inventory);
    }

    private ItemStack buildRandomItem(boolean selected) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((selected ? "§a✔ " : "§d") + "Random");

        if (selected) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        meta.setLore(List.of(
                "",
                "§7Use a random unlocked sword",
                "§7each match",
                "",
                selected ? "§aCurrently selected" : "§eClick to select"
        ));
        item.setItemMeta(meta);
        return item;
    }
}
