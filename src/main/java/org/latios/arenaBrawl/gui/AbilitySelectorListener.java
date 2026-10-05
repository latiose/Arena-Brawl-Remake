package org.latios.arenaBrawl.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityRegistry;
import org.latios.arenaBrawl.abilities.AbilitySelectionManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.hats.HatSelectorGUI;
import org.latios.arenaBrawl.runes.RuneSelectionManager;
import org.latios.arenaBrawl.runes.RuneType;
import org.latios.arenaBrawl.upgrades.CombatUpgradeGUI;

import java.util.ArrayList;
import java.util.List;

public class AbilitySelectorListener implements Listener {

    private final AbilityRegistry registry;
    private final AbilitySelectionManager selectionManager;
    private final AbilitySelectorGUI gui;
    private final RuneSelectionManager runeManager;
    private final HatSelectorGUI hatManager;
    private final CombatUpgradeGUI combatUpgradeGUI;

    public AbilitySelectorListener(AbilityRegistry registry, AbilitySelectionManager selectionManager, AbilitySelectorGUI gui, RuneSelectionManager runeManager, HatSelectorGUI hatSelectorGUI, CombatUpgradeGUI combatUpgradeGUI) {
        this.registry = registry;
        this.selectionManager = selectionManager;
        this.gui = gui;
        this.runeManager = runeManager;
        this.hatManager = hatSelectorGUI;
        this.combatUpgradeGUI = combatUpgradeGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AbilitySelectorGUI.AbilitySelectorHolder holder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if (event.getAction() == InventoryAction.NOTHING) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        int rawSlot = event.getRawSlot();

        if (holder.isRuneMenu()) {
            RuneType[] runes = RuneType.values();
            int totalPages = PaginationUtil.totalPages(runes.length);

            if (PaginationUtil.isPrevious(rawSlot, holder.page())) {
                gui.openRuneMenu(player, holder.page() - 1);
                return;
            }
            if (PaginationUtil.isNext(rawSlot, holder.page(), totalPages)) {
                gui.openRuneMenu(player, holder.page() + 1);
                return;
            }

            int index = PaginationUtil.indexOf(rawSlot, holder.page());
            if (index < 0 || index >= runes.length) return;

            RuneType chosen = runes[index];
            runeManager.select(player, chosen);
            player.sendMessage("§aRune set to: " + chosen.getDisplayName());
            player.closeInventory();
            return;
        }

        if (holder.slot() == null) {
            if (rawSlot == AbilitySelectorGUI.HATS_SLOT) {
                hatManager.open(player);
                return;
            }

            if (rawSlot == AbilitySelectorGUI.RUNE_SLOT) {
                gui.openRuneMenu(player);
                return;
            }

            if (rawSlot == AbilitySelectorGUI.UPGRADES_SLOT) {
                combatUpgradeGUI.open(player);
                return;
            }

            AbilitySlot slot = AbilitySelectorGUI.getMainMenuAbilitySlot(rawSlot);
            if (slot != null) {
                gui.openSlotMenu(player, slot);
            }
            return;
        }

        List<String> ids = new ArrayList<>(registry.getAvailableIds(holder.slot()));
        int totalPages = PaginationUtil.totalPages(ids.size());

        if (PaginationUtil.isPrevious(rawSlot, holder.page())) {
            gui.openSlotMenu(player, holder.slot(), holder.page() - 1);
            return;
        }
        if (PaginationUtil.isNext(rawSlot, holder.page(), totalPages)) {
            gui.openSlotMenu(player, holder.slot(), holder.page() + 1);
            return;
        }

        int index = PaginationUtil.indexOf(rawSlot, holder.page());
        if (index < 0 || index >= ids.size()) return;

        String chosenId = ids.get(index);
        selectionManager.select(player, holder.slot(), chosenId);

        Ability ability = registry.get(chosenId);
        String displayName = (ability != null) ? ability.getName() : chosenId;

        player.sendMessage("§a" + holder.slot().name() + " set to: " + displayName);
        player.closeInventory();
    }
}