// upgrades/CombatUpgradeGUI.java
package org.latios.arenaBrawl.upgrades;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CombatUpgradeGUI {

    public record CombatUpgradeHolder() implements InventoryHolder {
        @Override
        public Inventory getInventory() { throw new UnsupportedOperationException(); }
    }

    private final CombatUpgradeManager upgradeManager;

    public CombatUpgradeGUI(CombatUpgradeManager upgradeManager) {
        this.upgradeManager = upgradeManager;
    }

    /** Slot order matches CombatUpgradeType.values() order: HEALTH, ENERGY, MELEE_DAMAGE, COOLDOWN_REDUCTION */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new CombatUpgradeHolder(), 9, "§6§lCombat Upgrades");

        CombatUpgradeType[] types = CombatUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            inv.setItem(i * 2, buildItem(player, types[i]));
        }


        player.openInventory(inv);
    }

    private ItemStack buildItem(Player player, CombatUpgradeType type) {
        int level = upgradeManager.getLevel(player, type);
        double currentValue = type.getValueAtLevel(level);
        int nextCost = upgradeManager.getNextUpgradeCost(player, type);

        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + type.getDisplayName() + " §7(Level " + level + "/" + CombatUpgradeType.MAX_LEVEL + ")");

        String currentLine = "§7Current: §f" + formatValue(type, currentValue);
        String nextLine = level >= CombatUpgradeType.MAX_LEVEL
                ? "§aMax level reached!"
                : "§7Next level: §f" + formatValue(type, type.getValueAtLevel(level + 1))
                + " §8(§6" + nextCost + " coins§8)";

        meta.setLore(List.of(currentLine, nextLine, "", level >= CombatUpgradeType.MAX_LEVEL ? "" : "§eClick to upgrade"));
        item.setItemMeta(meta);
        return item;
    }

    private String formatValue(CombatUpgradeType type, double value) {
        return switch (type) {
            case HEALTH, ENERGY -> String.valueOf((int) value);
            case MELEE_DAMAGE -> String.format("%.2f", value);
            case COOLDOWN_REDUCTION -> String.format("%.2f%%", value);
        };
    }
}