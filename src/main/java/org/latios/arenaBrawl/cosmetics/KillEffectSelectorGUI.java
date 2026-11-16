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

public class KillEffectSelectorGUI {

    public record KillEffectHolder(int page) implements InventoryHolder {
        @Override
        public @NonNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }

    private static final int COINS_SLOT = 49;
    public static final int RANDOM_SLOT = 47;

    private final CosmeticEffectManager effectManager;
    private final StatsManager statsManager;

    public KillEffectSelectorGUI(CosmeticEffectManager effectManager, StatsManager statsManager) {
        this.effectManager = effectManager;
        this.statsManager = statsManager;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        KillEffectType[] effects = KillEffectType.values();
        int totalPages = PaginationUtil.totalPages(effects.length);
        int currentPage = PaginationUtil.clampPage(page, effects.length);

        Inventory inventory = Bukkit.createInventory(
                new KillEffectHolder(currentPage),
                PaginationUtil.INVENTORY_SIZE,
                PaginationUtil.title("§cKill Effects", currentPage, totalPages)
        );

        KillEffectType equipped = effectManager.isRandomKill(player) ? null : effectManager.getEquippedKillEffect(player);

        int start = PaginationUtil.firstIndex(currentPage);
        int end = PaginationUtil.lastIndexExclusive(currentPage, effects.length);

        for (int i = start; i < end; i++) {
            KillEffectType effect = effects[i];
            inventory.setItem(PaginationUtil.slotOf(i), buildDisplayItem(player, effect, equipped));
        }

        PaginationUtil.addNavigation(inventory, currentPage, totalPages);
        inventory.setItem(COINS_SLOT, buildCoinsItem(player));
        inventory.setItem(RANDOM_SLOT, buildRandomItem(effectManager.isRandomKill(player), "kill effect"));

        player.openInventory(inventory);
    }

    private ItemStack buildRandomItem(boolean selected, String label) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((selected ? "§a✔ " : "§d") + "Random");

        if (selected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.setLore(List.of(
                "",
                "§7Use a random unlocked " + label,
                "§7each match",
                "",
                selected ? "§aCurrently selected" : "§eClick to select"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildDisplayItem(Player player, KillEffectType effect, KillEffectType equipped) {
        boolean unlocked = effectManager.isKillUnlocked(player, effect);
        boolean isEquipped = effect == equipped;

        ItemStack item = new ItemStack(effect.getIcon());
        ItemMeta meta = item.getItemMeta();

        String prefix = isEquipped ? "§a✔ " : "";
        meta.setDisplayName(prefix + (unlocked ? effect.getColor() : "§7") + effect.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7" + effect.getDescription());
        lore.add("");
        lore.add("§8" + effect.getRarity() + " Kill Effect");

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
