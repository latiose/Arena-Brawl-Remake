package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Cosmetic particles that appear at a random spot around an enemy when a
 * melee hit deals damage.
 */
public enum HitEffectType {

    HEART("Heart", 1250, Material.POPPY,
            "A sweet burst of floating hearts."),
    HAPPY_VILLAGER("Happy Villager", 1500, Material.EMERALD,
            "Sparkling green happy villager particles."),
    CRIT_SPARK("Critical Spark", 2000, Material.IRON_SWORD,
            "Sharp critical hits and a sweeping arc."),
    FLAME("Flame Burst", 2500, Material.BLAZE_POWDER,
            "A fiery explosion of flames and embers."),
    SOUL_FLAME("Soul Flame", 3500, Material.SOUL_TORCH,
            "Eerie blue soul fire that clings to your enemy."),
    ELECTRIC("Electric Shock", 4000, Material.LIGHTNING_ROD,
            "Crackling electric sparks on every hit."),
    PORTAL("Void Rift", 5000, Material.ENDER_PEARL,
            "A tiny rift of portal particles opens on impact."),
    CHERRY("Cherry Blossom", 6000, Material.CHERRY_SAPLING,
            "A flurry of cherry petals and hearts."),
    NOTE("Musical", 7000, Material.NOTE_BLOCK,
            "Colourful musical notes burst from the hit."),
    SNOW("Snowburst", 8000, Material.SNOWBALL,
            "A freezing cloud of snowflakes."),
    TOTEM("Totem Spark", 10000, Material.TOTEM_OF_UNDYING,
            "Golden totem energy erupts around the victim."),
    DRAGON("Dragon Breath", 12500, Material.DRAGON_BREATH,
            "Swirling purple dragon breath and end rods."),
    BLOOD("Blood Splatter", 15000, Material.REDSTONE,
            "A dark crimson splatter on every hit."),
    ENCHANT("Arcane Runes", 18000, Material.ENCHANTING_TABLE,
            "Enchanting glyphs swirl around the impact."),
    COSMIC("Cosmic Impact", 22000, Material.NETHER_STAR,
            "Starlight and fireworks collide with your target."),
    GALAXY("Galaxy Collapse", 25000, Material.END_CRYSTAL,
            "A purple galaxy of portals, dust and starlight.");

    private final String displayName;
    private final int price;
    private final Material icon;
    private final String description;

    HitEffectType(String displayName, int price, Material icon, String description) {
        this.displayName = displayName;
        this.price = price;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPrice() {
        return price;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        if (price >= 20000) return "§d";
        if (price >= 10000) return "§6";
        if (price >= 5000) return "§b";
        return "§a";
    }

    public String getRarity() {
        if (price >= 20000) return "Legendary";
        if (price >= 10000) return "Epic";
        if (price >= 5000) return "Rare";
        return "Common";
    }

    public void play(Player attacker, Player victim, Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Location center = location.clone();

        switch (this) {
            case HEART -> {
                world.spawnParticle(Particle.HEART, center, 3, 0.2, 0.2, 0.2, 0.0);
            }
            case HAPPY_VILLAGER -> {
                world.spawnParticle(Particle.HAPPY_VILLAGER, center, 4, 0.2, 0.25, 0.2, 0.0);
            }
            case CRIT_SPARK -> {
                world.spawnParticle(Particle.CRIT, center, 5, 0.2, 0.25, 0.2, 0.05);
                world.spawnParticle(Particle.SWEEP_ATTACK, center, 1, 0, 0, 0, 0);
            }
            case FLAME -> {
                world.spawnParticle(Particle.FLAME, center, 5, 0.2, 0.25, 0.2, 0.01);
                world.spawnParticle(Particle.LAVA, center, 1, 0.1, 0.1, 0.1, 0.0);
            }
            case SOUL_FLAME -> {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 5, 0.2, 0.25, 0.2, 0.01);
            }
            case ELECTRIC -> {
                world.spawnParticle(Particle.ELECTRIC_SPARK, center, 6, 0.25, 0.3, 0.25, 0.05);
                world.spawnParticle(Particle.END_ROD, center, 1, 0.1, 0.1, 0.1, 0.01);
            }
            case PORTAL -> {
                world.spawnParticle(Particle.PORTAL, center, 6, 0.25, 0.3, 0.25, 0.05);
            }
            case CHERRY -> {
                world.spawnParticle(Particle.CHERRY_LEAVES, center, 5, 0.25, 0.3, 0.25, 0.01);
            }
            case NOTE -> {
                world.spawnParticle(Particle.NOTE, center, 4, 0.2, 0.25, 0.2, 1.0);
            }
            case SNOW -> {
                world.spawnParticle(Particle.SNOWFLAKE, center, 6, 0.25, 0.3, 0.25, 0.01);
                CosmeticEffects.dust(center, 3, 0.2, Color.fromRGB(190, 235, 255), 0.7f);
            }
            case TOTEM -> {
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 5, 0.2, 0.25, 0.2, 0.05);
            }
            case DRAGON -> {
                world.spawnParticle(Particle.DRAGON_BREATH, center, 6, 0.25, 0.3, 0.25, 0.02, 1.0f);
            }
            case BLOOD -> {
                CosmeticEffects.dust(center, 5, 0.2, Color.fromRGB(145, 0, 0), 0.8f);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, center, 1, 0.1, 0.1, 0.1, 0.0);
            }
            case ENCHANT -> {
                world.spawnParticle(Particle.ENCHANT, center, 7, 0.25, 0.35, 0.25, 0.1);
            }
            case COSMIC -> {
                world.spawnParticle(Particle.END_ROD, center, 5, 0.25, 0.3, 0.25, 0.04);
                world.spawnParticle(Particle.FIREWORK, center, 3, 0.2, 0.2, 0.2, 0.05);
            }
            case GALAXY -> {
                CosmeticEffects.dust(center, 6, 0.25, Color.fromRGB(140, 60, 255), 0.9f);
                world.spawnParticle(Particle.PORTAL, center, 4, 0.2, 0.25, 0.2, 0.05);
            }
        }
    }
}