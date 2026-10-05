package org.latios.arenaBrawl.upgrades;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CombatUpgradeGUI {

    private static final int INVENTORY_SIZE = 27;
    private static final int BAR_LENGTH = 10;
    private static final int ROW = 1;

    public record CombatUpgradeHolder() implements InventoryHolder {
        @Override
        public @NonNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }

    private final CombatUpgradeManager upgradeManager;

    public CombatUpgradeGUI(CombatUpgradeManager upgradeManager) {
        this.upgradeManager = upgradeManager;
    }

    public static int getSlotOf(int typeOrdinal) {
        int count = CombatUpgradeType.values().length;
        int width = (count - 1) * 2 + 1;
        int start = Math.max(0, (9 - width) / 2);
        return ROW * 9 + start + typeOrdinal * 2;
    }

    public static CombatUpgradeType getTypeAt(int rawSlot) {
        CombatUpgradeType[] types = CombatUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            if (getSlotOf(i) == rawSlot) {
                return types[i];
            }
        }
        return null;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new CombatUpgradeHolder(), INVENTORY_SIZE, "§6§lCombat Upgrades");

        CombatUpgradeType[] types = CombatUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            inv.setItem(getSlotOf(i), buildItem(player, types[i]));
        }

        player.openInventory(inv);
    }

    private ItemStack buildItem(Player player, CombatUpgradeType type) {
        int level = upgradeManager.getLevel(player, type);
        boolean maxed = level >= CombatUpgradeType.MAX_LEVEL;

        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6§l" + type.getDisplayName());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        if (maxed) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Level: §f" + level + "§8/§f" + CombatUpgradeType.MAX_LEVEL);
        lore.add(progressBar(level));
        lore.add("");
        lore.add("§7Current: §f" + formatValue(type, type.getValueAtLevel(level)));

        if (maxed) {
            lore.add("");
            lore.add("§a§lMAX LEVEL");
        } else {
            int nextCost = upgradeManager.getNextUpgradeCost(player, type);
            lore.add("§7Next: §a" + formatValue(type, type.getValueAtLevel(level + 1)));
            lore.add("");
            lore.add("§7Cost: §6" + nextCost + " coins");
            lore.add("");
            lore.add("§eClick to upgrade");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String progressBar(int level) {
        int filled = (int) Math.round(level * (double) BAR_LENGTH / CombatUpgradeType.MAX_LEVEL);
        filled = Math.max(0, Math.min(BAR_LENGTH, filled));

        StringBuilder bar = new StringBuilder("§8[§a");
        bar.append("■".repeat(filled));
        bar.append("§7");
        bar.append("■".repeat(BAR_LENGTH - filled));
        bar.append("§8]");
        return bar.toString();
    }

    private String formatValue(CombatUpgradeType type, double value) {
        return switch (type) {
            case HEALTH, ENERGY -> String.valueOf((int) value);
            case MELEE_DAMAGE -> String.format("%.2f", value);
            case COOLDOWN_REDUCTION -> String.format("%.2f%%", value);
        };
    }
}