package org.latios.arenaBrawl.abilities;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class AbilityIconRegistry {

    private final Map<String, Material> icons = new HashMap<>();
    private final Map<AbilitySlot, Material> fallbackBySlot = new java.util.EnumMap<>(AbilitySlot.class);

    public AbilityIconRegistry() {
        fallbackBySlot.put(AbilitySlot.OFFENSIVE, Material.IRON_SWORD);
        fallbackBySlot.put(AbilitySlot.UTILITY, Material.FEATHER);
        fallbackBySlot.put(AbilitySlot.SUPPORT, Material.GOLDEN_APPLE);
        fallbackBySlot.put(AbilitySlot.ULTIMATE, Material.NETHER_STAR);

        registerDefaults();
    }

    private void registerDefaults() {
        // --- OFFENSIVE ---
        icons.put("fireball", Material.FIRE_CHARGE);
        icons.put("snowball", Material.SNOWBALL);
        icons.put("melonlauncher", Material.MELON_SLICE);
        icons.put("pumpkinlauncher", Material.CARVED_PUMPKIN);
        icons.put("groundslam", Material.ANVIL);
        icons.put("freezingbreath", Material.BLUE_ICE);
        icons.put("evilbreath", Material.WITHER_ROSE);
        icons.put("heavensbreath", Material.GLOWSTONE_DUST);
        icons.put("dragonsbreath", Material.DRAGON_BREATH);
        icons.put("ancientbreath", Material.NETHERITE_SCRAP);
        icons.put("flamebreath", Material.BLAZE_POWDER);
        icons.put("lightningstrike", Material.TRIDENT);
        icons.put("consume", Material.ROTTEN_FLESH);
        icons.put("dash", Material.FEATHER);
        icons.put("rocketchicken", Material.CHICKEN_SPAWN_EGG);
        icons.put("mysticshot", Material.ARROW);
        icons.put("laywaste", Material.TNT);
        icons.put("particlebeam", Material.END_ROD);
        icons.put("burstfire", Material.CROSSBOW);
        icons.put("cookieshotgun", Material.COOKIE);
        icons.put("spikegrenade", Material.POINTED_DRIPSTONE);
        icons.put("void", Material.ENDER_EYE);
        icons.put("cowthrow", Material.LEATHER);
        icons.put("necromancy", Material.WITHER_SKELETON_SKULL);

        // --- UTILITY ---
        icons.put("shadowstep", Material.ENDER_PEARL);
        icons.put("salmonform", Material.SALMON);
        icons.put("sparkbolt", Material.GLOW_INK_SAC);
        icons.put("sugarrush", Material.SUGAR);
        icons.put("swap", Material.ENDER_EYE);
        icons.put("violentleap", Material.RABBIT_FOOT);
        icons.put("wallofvines", Material.VINE);
        icons.put("polymorph", Material.WHITE_WOOL);
        icons.put("barricade", Material.COBBLESTONE_WALL);
        icons.put("golemfall", Material.IRON_BLOCK);
        icons.put("bullcharge", Material.LEATHER_HORSE_ARMOR);
        icons.put("corruption", Material.CRYING_OBSIDIAN);
        icons.put("magneticImpulse", Material.LODESTONE);
        icons.put("RocketGrab", Material.FISHING_ROD);
        icons.put("darkpassage", Material.OBSIDIAN);
        icons.put("dodgeroll", Material.RABBIT_FOOT);
        icons.put("scavenger", Material.HOPPER);

        // --- SUPPORT ---
        icons.put("holywater", Material.POTION);
        icons.put("boneshield", Material.BONE);
        icons.put("cactusshield", Material.CACTUS);
        icons.put("starshield", Material.NETHER_STAR);
        icons.put("spongeshield", Material.SPONGE);
        icons.put("lifebond", Material.LEAD);
        icons.put("healingtotem", Material.BREWING_STAND);
        icons.put("treeoflife", Material.OAK_SAPLING);
        icons.put("songofpower", Material.NOTE_BLOCK);
        icons.put("lifeleech", Material.REDSTONE);
        icons.put("etheralbody", Material.PHANTOM_MEMBRANE);
        icons.put("healingrain", Material.WATER_BUCKET);
        icons.put("suzu", Material.BELL);
        icons.put("discordorb", Material.SHULKER_SHELL);
        icons.put("healingbeam", Material.BEACON);
        icons.put("berserkerrage", Material.REDSTONE_BLOCK);
        icons.put("magictable", Material.ENCHANTING_TABLE);
        icons.put("skittles", Material.RABBIT_STEW);

        // --- ULTIMATE ---
        icons.put("shieldwall", Material.SHIELD);
        icons.put("broodmother", Material.SPIDER_EYE);
        icons.put("absolutezero", Material.PACKED_ICE);
        icons.put("nanoboost", Material.GLOWSTONE);
        icons.put("divinejudgment", Material.TOTEM_OF_UNDYING);
        icons.put("healingwind", Material.ELYTRA);
        icons.put("thebox", Material.CHEST);
        icons.put("rewind", Material.CLOCK);
        icons.put("staticfield", Material.LIGHTNING_ROD);
        icons.put("myriadtruths", Material.ENCHANTED_BOOK);
        icons.put("arenadomain", Material.BEDROCK);
        icons.put("ninjadash", Material.STICK);
        icons.put("berserk", Material.BLAZE_ROD);
        icons.put("zombieapocalypse", Material.ZOMBIE_HEAD);
    }

    public Material getIcon(AbilitySlot slot, String id) {
        return icons.getOrDefault(id, fallbackBySlot.get(slot));
    }
}