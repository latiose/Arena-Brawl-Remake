package org.latios.arenaBrawl.hats;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.latios.arenaBrawl.gui.PaginationUtil;

import java.util.List;

public class HatSelectorListener implements Listener {

    private final HatSelectorGUI gui;
    private final HatSelectionManager hatSelectionManager;

    public HatSelectorListener(HatSelectorGUI gui, HatSelectionManager hatSelectionManager) {
        this.gui = gui;
        this.hatSelectionManager = hatSelectionManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HatSelectorGUI.HatSelectorHolder holder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        List<HatDefinition> hats = gui.getOrderedHats();
        int totalPages = PaginationUtil.totalPages(hats.size());
        int rawSlot = event.getRawSlot();

        if (PaginationUtil.isPrevious(rawSlot, holder.page())) {
            gui.open(player, holder.page() - 1);
            return;
        }
        if (PaginationUtil.isNext(rawSlot, holder.page(), totalPages)) {
            gui.open(player, holder.page() + 1);
            return;
        }

        int index = PaginationUtil.indexOf(rawSlot, holder.page());
        if (index < 0 || index >= hats.size()) return;

        HatDefinition clicked = hats.get(index);

        if (!hatSelectionManager.isUnlocked(player, clicked.id())) {
            player.sendMessage("§cYou haven't unlocked this hat yet!");
            return;
        }

        HatDefinition currentlyEquipped = hatSelectionManager.getEquipped(player);

        if (currentlyEquipped != null && currentlyEquipped.id().equals(clicked.id())) {
            hatSelectionManager.unequip(player);
            player.sendMessage("§eYou unequipped " + clicked.displayName() + ".");
        } else {
            hatSelectionManager.equip(player, clicked.id());
            player.sendMessage("§aYou equipped " + clicked.rarity().getColor() + clicked.displayName() + "§a!");
        }

        HatEquipUtils.applyEquippedHat(player, hatSelectionManager);
        gui.open(player, holder.page());
    }
}