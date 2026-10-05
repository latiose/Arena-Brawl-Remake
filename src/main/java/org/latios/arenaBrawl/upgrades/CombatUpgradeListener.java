package org.latios.arenaBrawl.upgrades;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CombatUpgradeListener implements Listener {

    private final CombatUpgradeGUI gui;
    private final CombatUpgradeManager upgradeManager;

    public CombatUpgradeListener(CombatUpgradeGUI gui, CombatUpgradeManager upgradeManager) {
        this.gui = gui;
        this.upgradeManager = upgradeManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CombatUpgradeGUI.CombatUpgradeHolder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        CombatUpgradeType type = CombatUpgradeGUI.getTypeAt(event.getRawSlot());
        if (type == null) return;

        CombatUpgradeManager.PurchaseResult result = upgradeManager.purchase(player, type);

        switch (result) {
            case SUCCESS -> player.sendMessage("§aUpgraded " + type.getDisplayName() + " to level "
                    + upgradeManager.getLevel(player, type) + "!");
            case NOT_ENOUGH_COINS -> player.sendMessage("§cYou don't have enough coins for this upgrade.");
            case MAXED_OUT -> player.sendMessage("§7This upgrade is already at max level.");
        }

        gui.open(player);
    }
}