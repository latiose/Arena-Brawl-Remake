// hats/HatDefinition.java
package org.latios.arenaBrawl.hats;

import org.bukkit.Color;
import org.bukkit.Material;

import java.util.List;
import java.util.UUID;

public record HatDefinition(
        String id,
        String displayName,
        Material material,
        HatRarity rarity,
        List<String> phrases,
        Color leatherColor,
        UUID playerHeadOwner // null unless this hat is a specific player's head
) {
    public HatDefinition(String id, String displayName, Material material, HatRarity rarity, List<String> phrases) {
        this(id, displayName, material, rarity, phrases, null, null);
    }

    public HatDefinition(String id, String displayName, Material material, HatRarity rarity,
                         List<String> phrases, Color leatherColor) {
        this(id, displayName, material, rarity, phrases, leatherColor, null);
    }

    public static HatDefinition playerHead(String id, String displayName, HatRarity rarity,
                                           List<String> phrases, UUID ownerUuid) {
        return new HatDefinition(id, displayName, Material.PLAYER_HEAD, rarity, phrases, null, ownerUuid);
    }
}