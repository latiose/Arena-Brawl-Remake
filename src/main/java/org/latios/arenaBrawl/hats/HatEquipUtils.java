
package org.latios.arenaBrawl.hats;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class HatEquipUtils {

    public static void applyEquippedHat(Player player, HatSelectionManager hatSelectionManager) {
        HatDefinition hat = hatSelectionManager.getEquipped(player);

        if (hat == null) {
            player.getInventory().setHelmet(null);
            return;
        }

        applyEquippedHat(player, hat, hat.material());
    }

    /**
     * Applies the equipped hat using a specific material. Used by animated hats,
     * which cycle through several materials while the hat is worn.
     */
    public static void applyEquippedHat(Player player, HatSelectionManager hatSelectionManager, Material material) {
        HatDefinition hat = hatSelectionManager.getEquipped(player);

        if (hat == null) {
            player.getInventory().setHelmet(null);
            return;
        }

        applyEquippedHat(player, hat, material);
    }

    private static void applyEquippedHat(Player player, HatDefinition hat, Material material) {
        ItemStack helmet = new ItemStack(material);
        ItemMeta meta = helmet.getItemMeta();
        meta.setDisplayName(hat.rarity().getColor() + hat.displayName());
        meta.setLore(buildPhraseLore(hat));
        meta.setUnbreakable(true);
        meta.addItemFlags(
                org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE
        );

        if (hat.enchanted()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        if (meta instanceof SkullMeta skullMeta && hat.playerHeadOwner() != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(hat.playerHeadOwner()));
        }
        if (meta instanceof LeatherArmorMeta leatherMeta && hat.leatherColor() != null) {
            leatherMeta.setColor(hat.leatherColor());
        }
        if (material.name().endsWith("_HELMET")) {
            meta.setAttributeModifiers(com.google.common.collect.ImmutableMultimap.of());
        }

        helmet.setItemMeta(meta);
        player.getInventory().setHelmet(helmet);
    }

    /** Builds the shared lore block listing this hat's rarity and its possible phrases. */
    public static List<String> buildPhraseLore(HatDefinition hat) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Rarity: " + hat.rarity().getColor() + hat.rarity().getDisplayName());
        lore.add("");
        lore.add("§eSayings:");
        for (String phrase : hat.phrases()) {
            lore.add("§f\"" + phrase + "\"");
        }
        return lore;
    }
}
