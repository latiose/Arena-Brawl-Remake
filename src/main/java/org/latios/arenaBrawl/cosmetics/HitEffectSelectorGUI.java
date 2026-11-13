package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import org.latios.arenaBrawl.gui.PaginationUtil;
import org.latios.arenaBrawl.stats.StatsManager;

import java.util.ArrayList;
import java.util.List;

public class HitEffectSelectorGUI {

    public record HitEffectHolder(int page) implements InventoryHolder {
        @Override
        public @NonNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }

    private static final int COINS_SLOT = 49;

    private final CosmeticEffectManager effectManager;
    private final StatsManager statsManager;

    public HitEffectSelectorGUI(CosmeticEffectManager effectManager, StatsManager statsManager) {
        this.effectManager = effectManager;
        this.statsManager = statsManager;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        HitEffectType[] effects = HitEffectType.values();
        int totalPages = PaginationUtil.totalPages(effects.length);
        int currentPage = PaginationUtil.clampPage(page, effects.length);

        Inventory inventory = Bukkit.createInventory(
                new HitEffectHolder(currentPage),
                PaginationUtil.INVENTORY_SIZE,
                PaginationUtil.title("§bHit Effects", currentPage, totalPages)
        );

        HitEffectType equipped = effectManager.getEquippedHitEffect(player);

        int start = PaginationUtil.firstIndex(currentPage);
        int end = PaginationUtil.lastIndexExclusive(currentPage, effects.length);

        for (int i = start; i < end; i++) {
            HitEffectType effect = effects[i];
            inventory.setItem(PaginationUtil.slotOf(i), buildDisplayItem(player, effect, equipped));
        }

        PaginationUtil.addNavigation(inventory, currentPage, totalPages);
        inventory.setItem(COINS_SLOT, buildCoinsItem(player));

        player.openInventory(inventory);
    }

    private ItemStack buildDisplayItem(Player player, HitEffectType effect, HitEffectType equipped) {
        boolean unlocked = effectManager.isHitUnlocked(player, effect);
        boolean isEquipped = effect == equipped;

        ItemStack item = new ItemStack(effect.getIcon());
        ItemMeta meta = item.getItemMeta();

        String prefix = isEquipped ? "§a✔ " : "";
        meta.setDisplayName(prefix + (unlocked ? effect.getColor() : "§7") + effect.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7" + effect.getDescription());
        lore.add("");
        lore.add("§8" + effect.getRarity() + " Hit Effect");

        if (unlocked) {
            lore.add("");
            lore.add(isEquipped ? "§aCurrently equipped" : "§eClick to equip");
            if (isEquipped) {
                lore.add("§7Click again to disable");
            }

            if (isEquipped) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        } else {
            lore.add("");
            lore.add("§7Price: §6" + effect.getPrice() + " coins");
            lore.add("§eClick to buy & equip");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCoinsItem(Player player) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Your Coins");
        meta.setLore(List.of("", "§f" + statsManager.getStats(player).coins));
        item.setItemMeta(meta);
        return item;
    }
}
