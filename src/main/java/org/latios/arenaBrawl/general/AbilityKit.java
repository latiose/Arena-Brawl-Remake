package org.latios.arenaBrawl.general;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.cosmetics.SwordSelectionManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AbilityKit {

    private static final Map<AbilitySlot, Material> SLOT_ICONS = new EnumMap<>(AbilitySlot.class);
    static {
        SLOT_ICONS.put(AbilitySlot.OFFENSIVE, Material.IRON_SWORD);
        SLOT_ICONS.put(AbilitySlot.UTILITY, Material.GLOWSTONE_DUST);
        SLOT_ICONS.put(AbilitySlot.SUPPORT, Material.LIME_DYE);
        SLOT_ICONS.put(AbilitySlot.ULTIMATE, Material.ORANGE_DYE);
    }

    public static Material getIcon(AbilitySlot slot) {
        return SLOT_ICONS.get(slot);
    }

    public static Material getIcon(Player player, AbilitySlot slot, SwordSelectionManager swordSelectionManager) {
        if (slot == AbilitySlot.OFFENSIVE) {
            return swordSelectionManager.getEquipped(player).getMaterial();
        }
        return getIcon(slot);
    }

    public static void giveDefaultKit(Player player, AbilityManager abilityManager,
                                      SwordSelectionManager swordSelectionManager) {
        player.getInventory().clear();

        for (AbilitySlot slot : AbilitySlot.values()) {
            Ability ability = abilityManager.getAbility(player, slot);
            String name = ability != null ? ability.getName() : slot.name();
            player.getInventory().setItem(slot.ordinal(),
                    namedItem(getIcon(player, slot, swordSelectionManager), "§e" + name, ability));
        }
    }

    private static ItemStack namedItem(Material material, String name, Ability ability) {
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = buildAbilityLore(ability);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }

        if (material.name().endsWith("_SWORD")) {
            item.setData(
                    DataComponentTypes.CONSUMABLE,
                    Consumable.consumable()
                            .consumeSeconds(72000.0f)
                            .animation(ItemUseAnimation.BLOCK)
                            .hasConsumeParticles(false)
                            .build()
            );
        }

        return item;
    }

    public static ItemStack createAbilityItem(Material material, String name) {
        return createAbilityItem(material, name, null);
    }

    public static ItemStack createAbilityItem(Material material, String name, Ability ability) {
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);

            List<String> lore = buildAbilityLore(ability);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        if (material.name().endsWith("_SWORD")) {
            item.setData(
                    DataComponentTypes.CONSUMABLE,
                    Consumable.consumable()
                            .consumeSeconds(72000.0f)
                            .animation(ItemUseAnimation.BLOCK)
                            .hasConsumeParticles(false)
                            .build()
            );
        }

        return item;
    }

    /** Builds the item tooltip shown in the match kit: wrapped ability description. */
    private static List<String> buildAbilityLore(Ability ability) {
        if (ability == null) return List.of();

        String description = ability.getDescription();
        if (description == null || description.isBlank()) return List.of();

        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : wrapText(description, 40)) {
            lore.add("§7" + line);
        }
        return lore;
    }

    /** Splits text into lines of at most {@code lineLength} characters without breaking words. */
    public static List<String> wrapText(String text, int lineLength) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > lineLength) {
                if (!currentLine.isEmpty()) {
                    result.add(currentLine.toString());
                    currentLine.setLength(0);
                }
            }
            if (!currentLine.isEmpty()) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (!currentLine.isEmpty()) {
            result.add(currentLine.toString());
        }

        return result;
    }
}