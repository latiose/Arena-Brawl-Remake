// gui/AbilitySelectorGUI.java
package org.latios.arenaBrawl.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.latios.arenaBrawl.abilities.AbilityRegistry;
import org.latios.arenaBrawl.abilities.AbilitySelectionManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;

import java.util.ArrayList;
import java.util.List;

public class AbilitySelectorGUI {

    private final AbilityRegistry registry;
    private final AbilitySelectionManager selectionManager;

    public AbilitySelectorGUI(AbilityRegistry registry, AbilitySelectionManager selectionManager) {
        this.registry = registry;
        this.selectionManager = selectionManager;
    }

    public void openSlotMenu(Player player, AbilitySlot slot) {
        List<String> ids = new ArrayList<>(registry.getAvailableIds(slot));

        Inventory inv = Bukkit.createInventory(new AbilitySelectorHolder(slot), 27, "Select: " + slot.name());

        String current = selectionManager.getSelection(player, slot);

        for (int i = 0; i < ids.size() && i < 27; i++) {
            String id = ids.get(i);
            boolean selected = id.equals(current);

            ItemStack item = new ItemStack(selected ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((selected ? "§a✔ " : "§f") + prettify(id));
            item.setItemMeta(meta);

            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new AbilitySelectorHolder(null), 9, "Ability Selector");

        for (AbilitySlot slot : AbilitySlot.values()) {
            String current = selectionManager.getSelection(player, slot);
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + slot.name());
            meta.setLore(List.of("§7Actual: §f" + prettify(current)));
            item.setItemMeta(meta);
            inv.setItem(slot.ordinal() * 2, item); // slots 0,2,4,6
        }

        player.openInventory(inv);
    }

    private String prettify(String id) {
        return id.substring(0, 1).toUpperCase() + id.substring(1);
    }

    public record AbilitySelectorHolder(AbilitySlot slot) implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }
}