
package org.latios.arenaBrawl.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.latios.arenaBrawl.abilities.AbilityRegistry;
import org.latios.arenaBrawl.abilities.AbilitySelectionManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.hats.HatSelectorGUI;
import org.latios.arenaBrawl.runes.RuneSelectionManager;
import org.latios.arenaBrawl.runes.RuneType;

import java.util.ArrayList;
import java.util.List;

public class AbilitySelectorGUI {
    private final RuneSelectionManager runeSelectionManager;
    private final AbilityRegistry registry;
    private final AbilitySelectionManager selectionManager;
    private final HatSelectorGUI hatSelectorGUI;
    public AbilitySelectorGUI(AbilityRegistry registry, AbilitySelectionManager selectionManager,RuneSelectionManager runeSelectionManager, HatSelectorGUI hatSelectorGUI) {
        this.registry = registry;
        this.selectionManager = selectionManager;
        this.runeSelectionManager = runeSelectionManager;
        this.hatSelectorGUI = hatSelectorGUI;
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
        Inventory inv = Bukkit.createInventory(new AbilitySelectorHolder(null), 9, "Selection Menu");

        for (AbilitySlot slot : AbilitySlot.values()) {
            String current = selectionManager.getSelection(player, slot);
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + slot.name());
            meta.setLore(List.of("§7Current: §f" + prettify(current)));
            item.setItemMeta(meta);
            inv.setItem(slot.ordinal() * 2, item); // slots 0, 2, 4, 6
        }

        // Rune section, placed in the last slot
        RuneType currentRune = runeSelectionManager.getSelection(player);
        ItemStack runeItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta runeMeta = runeItem.getItemMeta();
        runeMeta.setDisplayName("§dRune");
        runeMeta.setLore(List.of("§7Current: §f" + currentRune.getDisplayName()));
        runeItem.setItemMeta(runeMeta);
        inv.setItem(8, runeItem);

        ItemStack hatItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta hatMeta = hatItem.getItemMeta();
        hatMeta.setDisplayName("§dHats");
        hatMeta.setLore(List.of("§7Click to choose your hat"));
        hatItem.setItemMeta(hatMeta);
        inv.setItem(7, hatItem);

        player.openInventory(inv);
    }

    /** Opens the rune selection submenu, mirroring openSlotMenu's structure for ability slots. */
    public void openRuneMenu(Player player) {
        RuneType[] runes = RuneType.values();
        Inventory inv = Bukkit.createInventory(new AbilitySelectorHolder(null, true), 27, "Choose a Rune");

        RuneType current = runeSelectionManager.getSelection(player);

        for (int i = 0; i < runes.length; i++) {
            RuneType rune = runes[i];
            boolean selected = rune == current;

            ItemStack item = new ItemStack(selected ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((selected ? "§a✔ " : "§f") + rune.getDisplayName());
            meta.setLore(List.of("§7Proc chance: §f" + (int) (rune.getProcChance() * 100) + "%"));
            item.setItemMeta(meta);

            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }



    private String prettify(String id) {
        return id.substring(0, 1).toUpperCase() + id.substring(1);
    }

    public record AbilitySelectorHolder(AbilitySlot slot, boolean isRuneMenu) implements InventoryHolder {
        public AbilitySelectorHolder(AbilitySlot slot) {
            this(slot, false);
        }

        @Override
        public Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }
}