
package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Material;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.latios.arenaBrawl.rating.RatingManager;

import java.util.List;

public class ArmorTierManager {

    private final RatingManager ratingManager;
    private static final int TOP_SIZE_FOR_GLOW = 10;

    public ArmorTierManager(RatingManager ratingManager) {
        this.ratingManager = ratingManager;
    }

    public ArmorTier resolveTier(Player player) {
        double rating = ratingManager.getRating(player);

        if (isInTop10(player)) {
            return ArmorTier.DIAMOND_TOP10;
        }
        if (rating >= 2000) return ArmorTier.DIAMOND;
        if (rating >= 1700) return ArmorTier.GOLD;
        if (rating >= 1300) return ArmorTier.IRON;
        return ArmorTier.LEATHER;
    }

    private boolean isInTop10(Player player) {
        List<java.util.Map.Entry<String, Double>> top = ratingManager.getTopRatings(TOP_SIZE_FOR_GLOW);
        return top.stream().anyMatch(entry -> entry.getKey().equals(player.getName()));
    }

    public void equipCosmeticArmor(Player player) {
        ArmorTier tier = resolveTier(player);

        player.getInventory().setBoots(buildPiece(tier.getBoots(), tier));
        player.getInventory().setLeggings(buildPiece(tier.getLeggings(), tier));
        player.getInventory().setChestplate(buildPiece(tier.getChestplate(), tier));
    }



    private ItemStack buildPiece(Material material, ArmorTier tier) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§f" + tier.getTierName());
        meta.setLore(List.of(
                "§7Cosmetic armor - no defensive bonus",
                "§7" + tier.getRequirement()
        ));

        meta.setAttributeModifiers(com.google.common.collect.ImmutableMultimap.of());
        meta.setUnbreakable(true);

        if (tier.isTop10()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        //meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }
}