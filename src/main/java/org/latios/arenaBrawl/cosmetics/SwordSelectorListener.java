package org.latios.arenaBrawl.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SwordSelectorListener implements Listener {
    private final SwordSelectorGUI gui;
    private final SwordSelectionManager selectionManager;

    public SwordSelectorListener(SwordSelectorGUI gui, SwordSelectionManager selectionManager) {
        this.gui = gui;
        this.selectionManager = selectionManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SwordSelectorGUI.SwordSelectorHolder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int index = event.getRawSlot() - 10;
        SwordType[] swords = SwordType.values();
        if (index < 0 || index >= swords.length) return;

        SwordType clicked = swords[index];
        if (!selectionManager.isUnlocked(player, clicked)) {
            player.sendMessage("§cYou haven't unlocked this sword yet!");
            return;
        }
        selectionManager.equip(player, clicked);
        player.sendMessage("§aYou equipped " + clicked.getDisplayName() + "§a!");
        gui.open(player);
    }
}
