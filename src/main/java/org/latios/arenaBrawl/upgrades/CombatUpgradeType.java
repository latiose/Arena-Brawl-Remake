
package org.latios.arenaBrawl.upgrades;

import org.bukkit.Material;

public enum CombatUpgradeType {

    HEALTH("Max Health", Material.GOLDEN_APPLE, 2000.0,
            new double[]{2022, 2044, 2066, 2088, 2111, 2133, 2155, 2177, 2200}),

    ENERGY("Max Energy", Material.GLOWSTONE_DUST, 100.0,
            new double[]{102, 104, 106, 108, 111, 113, 115, 117, 120}),

    MELEE_DAMAGE("Melee Damage", Material.IRON_SWORD, 10.0,
            new double[]{10.22, 10.44, 10.67, 10.89, 11.11, 11.33, 11.56, 11.78, 12.0}),

    COOLDOWN_REDUCTION("Cooldown Reduction", Material.CLOCK, 0.0,
            new double[]{1.11, 2.22, 3.33, 4.44, 5.56, 6.67, 7.78, 8.89, 10.0});

    public static final int MAX_LEVEL = 9;
    private static final int COST_BASE = 880;

    private final String displayName;
    private final Material icon;
    private final double baseValue;
    private final double[] valuesByLevel;

    CombatUpgradeType(String displayName, Material icon, double baseValue, double[] valuesByLevel) {
        this.displayName = displayName;
        this.icon = icon;
        this.baseValue = baseValue;
        this.valuesByLevel = valuesByLevel;
    }

    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public double getBaseValue() { return baseValue; }

    public double getValueAtLevel(int level) {
        if (level <= 0) return baseValue;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        return valuesByLevel[level - 1];
    }

    public static int getCostForLevel(int nextLevel) {
        return COST_BASE * nextLevel * nextLevel;
    }
}