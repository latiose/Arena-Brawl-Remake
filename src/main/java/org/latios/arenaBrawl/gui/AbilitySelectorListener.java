// gui/AbilitySelectorListener.java
package org.latios.arenaBrawl.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.latios.arenaBrawl.abilities.AbilityRegistry;
import org.latios.arenaBrawl.abilities.AbilitySelectionManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.hats.HatSelectionManager;
import org.latios.arenaBrawl.hats.HatSelectorGUI;
import org.latios.arenaBrawl.runes.RuneSelectionManager;
import org.latios.arenaBrawl.runes.RuneType;

import java.util.ArrayList;
import java.util.List;

public class AbilitySelectorListener implements Listener {

    private final AbilityRegistry registry;
    private final AbilitySelectionManager selectionManager;
    private final AbilitySelectorGUI gui;
    private final RuneSelectionManager runeManager;
    private final HatSelectorGUI hatManager;
    public AbilitySelectorListener(AbilityRegistry registry, AbilitySelectionManager selectionManager, AbilitySelectorGUI gui, RuneSelectionManager runeManager,HatSelectorGUI hatSelectorGUI) {
        this.registry = registry;
        this.selectionManager = selectionManager;
        this.gui = gui;
        this.runeManager = runeManager;
        this.hatManager = hatSelectorGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AbilitySelectorGUI.AbilitySelectorHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        if (holder.isRuneMenu()) {
            int index = event.getRawSlot();
            RuneType[] runes = RuneType.values();
            if (index < 0 || index >= runes.length) return;

            RuneType chosen = runes[index];
            runeManager.select(player, chosen);
            player.sendMessage("§dRune set to: " + chosen.getDisplayName());
            player.closeInventory();
            return;
        }

        if (holder.slot() == null) {
            int rawSlot = event.getRawSlot();

            if (rawSlot == 7) { // hat item
                hatManager.open(player);
                return;
            }

            if (rawSlot == 8) { // rune item
                gui.openRuneMenu(player);
                return;
            }

            AbilitySlot slot = switch (rawSlot) {
                case 0 -> AbilitySlot.OFFENSIVE;
                case 2 -> AbilitySlot.UTILITY;
                case 4 -> AbilitySlot.SUPPORT;
                case 6 -> AbilitySlot.ULTIMATE;
                default -> null;
            };
            if (slot != null) {
                gui.openSlotMenu(player, slot);
            }
            return;
        }

        List<String> ids = new ArrayList<>(registry.getAvailableIds(holder.slot()));
        int index = event.getRawSlot();
        if (index < 0 || index >= ids.size()) return;

        String chosenId = ids.get(index);
        selectionManager.select(player, holder.slot(), chosenId);
        player.sendMessage("§a" + holder.slot().name() + " set to: " + chosenId);
        player.closeInventory();
    }
}