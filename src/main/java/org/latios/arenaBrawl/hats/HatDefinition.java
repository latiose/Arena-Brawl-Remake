package org.latios.arenaBrawl.hats;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.UUID;

public record HatDefinition(
        String id,
        String displayName,
        Material material,
        HatRarity rarity,
        List<String> phrases,
        Color leatherColor,
        UUID playerHeadOwner
) {
    public HatDefinition(String id, String displayName, Material material, HatRarity rarity, List<String> phrases) {
        this(id, displayName, material, rarity, phrases, null, null);
    }

    public HatDefinition(String id, String displayName, Material material, HatRarity rarity,
                         List<String> phrases, Color leatherColor) {
        this(id, displayName, material, rarity, phrases, leatherColor, null);
    }

    public HatDefinition(String id, ConfigurationSection section) {
        this(
                id,
                section.getString("name", id),
                parseMaterial(section.getString("material")),
                parseRarity(section.getString("rarity")),
                section.getStringList("lore"),
                parseColor(section.getString("color")),
                parseUuid(section.getString("texture-uuid"))
        );
    }

    public static HatDefinition playerHead(String id, String displayName, HatRarity rarity,
                                           List<String> phrases, UUID ownerUuid) {
        return new HatDefinition(id, displayName, Material.PLAYER_HEAD, rarity, phrases, null, ownerUuid);
    }

    // Métodos auxiliares de parsing
    private static Material parseMaterial(String matName) {
        if (matName == null) return Material.DIRT;
        Material mat = Material.matchMaterial(matName);
        return mat != null ? mat : Material.DIRT;
    }

    private static HatRarity parseRarity(String rarityStr) {
        if (rarityStr == null) return HatRarity.COMMON;
        try {
            return HatRarity.valueOf(rarityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return HatRarity.COMMON;
        }
    }

    private static Color parseColor(String hex) {
        if (hex == null || hex.isEmpty()) return null;
        try {
            int rgb = Integer.parseInt(hex.replace("#", ""), 16);
            return Color.fromRGB(rgb);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static UUID parseUuid(String uuidStr) {
        if (uuidStr == null || uuidStr.isEmpty()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}