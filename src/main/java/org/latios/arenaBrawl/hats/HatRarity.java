
package org.latios.arenaBrawl.hats;

import org.bukkit.ChatColor;

public enum HatRarity {
    COMMON(ChatColor.GRAY, "Common"),
    RARE(ChatColor.BLUE, "Rare"),
    EPIC(ChatColor.DARK_PURPLE, "Epic");

    private final ChatColor color;
    private final String displayName;

    HatRarity(ChatColor color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public ChatColor getColor() { return color; }
    public String getDisplayName() { return displayName; }
}