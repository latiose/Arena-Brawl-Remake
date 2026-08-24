// upgrades/CombatUpgradeListener.java
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
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();
        CombatUpgradeType[] types = CombatUpgradeType.values();

        int index = slot / 2;
        if (slot % 2 != 0 || index >= types.length) return; // ignore filler/coins slot

        CombatUpgradeType type = types[index];
        CombatUpgradeManager.PurchaseResult result = upgradeManager.purchase(player, type);

        switch (result) {
            case SUCCESS -> player.sendMessage("§aUpgraded " + type.getDisplayName() + " to level "
                    + upgradeManager.getLevel(player, type) + "!");
            case NOT_ENOUGH_COINS -> player.sendMessage("§cYou don't have enough coins for this upgrade.");
            case MAXED_OUT -> player.sendMessage("§7This upgrade is already at max level.");
        }

        gui.open(player); // refresh to show updated level/cost
    }
}