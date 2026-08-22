package org.latios.arenaBrawl.general;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;

import java.util.EnumMap;
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

    public static void giveDefaultKit(Player player, AbilityManager abilityManager) {
        player.getInventory().clear();

        for (AbilitySlot slot : AbilitySlot.values()) {
            Ability ability = abilityManager.getAbility(player, slot);
            String name = ability != null ? ability.getName() : slot.name();
            player.getInventory().setItem(slot.ordinal(), namedItem(SLOT_ICONS.get(slot), "§e" + name));
        }
    }

    private static ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}