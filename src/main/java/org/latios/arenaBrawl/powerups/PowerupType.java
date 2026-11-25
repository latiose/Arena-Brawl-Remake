package org.latios.arenaBrawl.powerups;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum PowerupType {
    HEALTH(Material.EMERALD_BLOCK, "HEALING POWERUP", "HEALING",
            NamedTextColor.GREEN, "§a", new PowerupSchedule(95_000, 30_000, 45_000)),
    DAMAGE(Material.REDSTONE_BLOCK, "DAMAGE POWERUP", "DAMAGE",
            NamedTextColor.RED, "§c", new PowerupSchedule(95_000, 65_000, 90_000)),
    /**
     * Personal variant of the damage powerup. It shares the damage spawn,
     * location and schedule; it is only used as a visual/effect descriptor
     * for players who enabled it in the menu.
     */
    ENERGY(Material.DIAMOND_BLOCK, "ENERGY POWERUP", "ENERGY",
            NamedTextColor.AQUA, "§b", new PowerupSchedule(95_000, 65_000, 90_000));

    private final Material material;
    private final String displayName;
    private final String label;
    private final NamedTextColor color;
    private final String legacyColor;
    private final PowerupSchedule schedule;

    PowerupType(Material material, String displayName, String label,
                NamedTextColor color, String legacyColor, PowerupSchedule schedule) {
        this.material = material;
        this.displayName = displayName;
        this.label = label;
        this.color = color;
        this.legacyColor = legacyColor;
        this.schedule = schedule;
    }

    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String getLabel() { return label; }
    public NamedTextColor getColor() { return color; }
    public String getLegacyColor() { return legacyColor; }
    public PowerupSchedule getSchedule() { return schedule; }
}
