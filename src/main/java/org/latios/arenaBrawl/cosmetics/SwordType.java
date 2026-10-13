package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Material;

public enum SwordType {
    WOOD(Material.WOODEN_SWORD, "Wooden Sword"),
    STONE(Material.STONE_SWORD, "Stone Sword"),
    IRON(Material.IRON_SWORD, "Iron Sword"),
    GOLD(Material.GOLDEN_SWORD, "Golden Sword"),
    DIAMOND(Material.DIAMOND_SWORD, "Diamond Sword"),
    NETHERITE(Material.NETHERITE_SWORD, "Netherite Sword");

    private final Material material;
    private final String displayName;

    SwordType(Material material, String displayName) {
        this.material = material;
        this.displayName = displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }
}
