// powerups/PowerupType.java
package org.latios.arenaBrawl.powerups;

import org.bukkit.Material;

public enum PowerupType {
    HEALTH(Material.EMERALD_BLOCK, "HEALING POWERUP", new PowerupSchedule(95_000, 30_000, 45_000)),
    DOUBLE_DAMAGE(Material.REDSTONE_BLOCK, "DOUBLE DAMAGE POWERUP", new PowerupSchedule(95_000, 65_000, 90_000));

    private final Material material;
    private final String displayName;
    private final PowerupSchedule schedule;

    PowerupType(Material material, String displayName, PowerupSchedule schedule) {
        this.material = material;
        this.displayName = displayName;
        this.schedule = schedule;
    }

    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public PowerupSchedule getSchedule() { return schedule; }
}