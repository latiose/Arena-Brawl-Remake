package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Material;

public enum ArmorTier {

    LEATHER(Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE,
            "Bronze Tier", "Rating below 1300"),
    IRON(Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE,
            "Silver Tier", "Rating 1300 - 1699"),
    GOLD(Material.GOLDEN_BOOTS, Material.GOLDEN_LEGGINGS, Material.GOLDEN_CHESTPLATE,
            "Gold Tier", "Rating 1700 - 1999"),
    DIAMOND(Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE,
            "Diamond Tier", "Rating 2000+"),
    DIAMOND_TOP10(Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE,
            "Diamond Tier (Top 10)", "Top 10 player worldwide");

    private final Material boots;
    private final Material leggings;
    private final Material chestplate;
    private final String tierName;
    private final String requirement;

    ArmorTier(Material boots, Material leggings, Material chestplate, String tierName, String requirement) {
        this.boots = boots;
        this.leggings = leggings;
        this.chestplate = chestplate;
        this.tierName = tierName;
        this.requirement = requirement;
    }

    public Material getBoots() { return boots; }
    public Material getLeggings() { return leggings; }
    public Material getChestplate() { return chestplate; }
    public String getTierName() { return tierName; }
    public String getRequirement() { return requirement; }
    public boolean isTop10() { return this == DIAMOND_TOP10; }
}