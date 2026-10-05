package org.latios.arenaBrawl.hats;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jspecify.annotations.NonNull;
import org.latios.arenaBrawl.gui.PaginationUtil;

import java.util.ArrayList;
import java.util.List;

public class HatSelectorGUI {

    public record HatSelectorHolder(int page) implements InventoryHolder {
        @Override
        public @NonNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }

    private final HatRegistry hatRegistry;
    private final HatSelectionManager hatSelectionManager;

    public HatSelectorGUI(HatRegistry hatRegistry, HatSelectionManager hatSelectionManager) {
        this.hatRegistry = hatRegistry;
        this.hatSelectionManager = hatSelectionManager;
    }

    public List<HatDefinition> getOrderedHats() {
        List<HatDefinition> ordered = new ArrayList<>();
        ordered.addAll(hatRegistry.getByRarity(HatRarity.COMMON));
        ordered.addAll(hatRegistry.getByRarity(HatRarity.RARE));
        ordered.addAll(hatRegistry.getByRarity(HatRarity.EPIC));
        return ordered;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<HatDefinition> hats = getOrderedHats();
        int totalPages = PaginationUtil.totalPages(hats.size());
        int currentPage = PaginationUtil.clampPage(page, hats.size());

        Inventory inv = Bukkit.createInventory(
                new HatSelectorHolder(currentPage),
                PaginationUtil.INVENTORY_SIZE,
                PaginationUtil.title("§dHat Selector", currentPage, totalPages));

        HatDefinition equipped = hatSelectionManager.getEquipped(player);

        int start = PaginationUtil.firstIndex(currentPage);
        int end = PaginationUtil.lastIndexExclusive(currentPage, hats.size());

        for (int i = start; i < end; i++) {
            HatDefinition hat = hats.get(i);
            boolean unlocked = hatSelectionManager.isUnlocked(player, hat.id());
            boolean isEquipped = equipped != null && equipped.id().equals(hat.id());

            inv.setItem(PaginationUtil.slotOf(i), buildDisplayItem(hat, unlocked, isEquipped));
        }

        PaginationUtil.addNavigation(inv, currentPage, totalPages);
        player.openInventory(inv);
    }

    private ItemStack buildDisplayItem(HatDefinition hat, boolean unlocked, boolean isEquipped) {
        Material displayMaterial = unlocked ? hat.material() : Material.GRAY_DYE;

        ItemStack item = new ItemStack(displayMaterial);
        ItemMeta meta = item.getItemMeta();

        if (unlocked) {
            String prefix = isEquipped ? "§a✔ " : "";
            meta.setDisplayName(prefix + hat.rarity().getColor() + hat.displayName());

            List<String> lore = new ArrayList<>(HatEquipUtils.buildPhraseLore(hat));
            lore.add("");
            lore.add(isEquipped ? "§aCurrently equipped" : "§eClick to equip");
            meta.setLore(lore);

            if (meta instanceof SkullMeta skullMeta && hat.playerHeadOwner() != null) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(hat.playerHeadOwner()));
            }
            if (meta instanceof LeatherArmorMeta leatherMeta && hat.leatherColor() != null) {
                leatherMeta.setColor(hat.leatherColor());
            }
        } else {
            meta.setDisplayName("§7??? " + hat.rarity().getColor() + hat.rarity().getDisplayName() + " §7Hat");
            meta.setLore(List.of(
                    "",
                    "§7Not unlocked yet.",
                    "§7Find it in the §dMagic Chest§7!"
            ));
        }

        item.setItemMeta(meta);
        return item;
    }
}