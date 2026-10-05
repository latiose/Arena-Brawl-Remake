package org.latios.arenaBrawl.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class PaginationUtil {

    public static final int INVENTORY_SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int PREVIOUS_PAGE_SLOT = 48;
    public static final int NEXT_PAGE_SLOT = 50;

    private PaginationUtil() {
    }

    public static int totalPages(int itemCount) {
        return Math.max(1, (int) Math.ceil(itemCount / (double) ITEMS_PER_PAGE));
    }

    public static int clampPage(int page, int itemCount) {
        return Math.max(0, Math.min(page, totalPages(itemCount) - 1));
    }

    public static int firstIndex(int page) {
        return page * ITEMS_PER_PAGE;
    }

    public static int lastIndexExclusive(int page, int itemCount) {
        return Math.min(itemCount, firstIndex(page) + ITEMS_PER_PAGE);
    }

    public static int slotOf(int index) {
        return index % ITEMS_PER_PAGE;
    }

    public static int indexOf(int rawSlot, int page) {
        if (rawSlot < 0 || rawSlot >= ITEMS_PER_PAGE) {
            return -1;
        }
        return firstIndex(page) + rawSlot;
    }

    public static String title(String base, int page, int totalPages) {
        return totalPages > 1 ? base + " (" + (page + 1) + "/" + totalPages + ")" : base;
    }

    public static void addNavigation(Inventory inv, int page, int totalPages) {
        if (page > 0) {
            inv.setItem(PREVIOUS_PAGE_SLOT, arrow("§ePrevious Page", page, totalPages));
        }
        if (page < totalPages - 1) {
            inv.setItem(NEXT_PAGE_SLOT, arrow("§eNext Page", page + 2, totalPages));
        }
    }

    public static boolean isPrevious(int rawSlot, int page) {
        return rawSlot == PREVIOUS_PAGE_SLOT && page > 0;
    }

    public static boolean isNext(int rawSlot, int page, int totalPages) {
        return rawSlot == NEXT_PAGE_SLOT && page < totalPages - 1;
    }

    private static ItemStack arrow(String name, int targetPage, int totalPages) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("", "§7Page §f" + targetPage + "§7/§f" + totalPages));
        arrow.setItemMeta(meta);
        return arrow;
    }
}