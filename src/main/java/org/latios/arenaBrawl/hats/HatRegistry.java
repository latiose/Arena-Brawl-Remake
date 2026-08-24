// hats/HatRegistry.java
package org.latios.arenaBrawl.hats;

import org.bukkit.Color;
import org.bukkit.Material;

import java.util.*;

public class HatRegistry {

    private final Map<String, HatDefinition> hatsById = new LinkedHashMap<>();
    private final Map<HatRarity, List<HatDefinition>> hatsByRarity = new EnumMap<>(HatRarity.class);

    public HatRegistry() {
        for (HatRarity rarity : HatRarity.values()) {
            hatsByRarity.put(rarity, new ArrayList<>());
        }
        registerCommonHats();
        registerRareHats();
        registerEpicHats();
    }

    private void registerCommonHats() {
        register(new HatDefinition("troll", "Troll Hat", Material.BEDROCK, HatRarity.COMMON, List.of(
                "umadbro?", "Trolol!", "try harder bro"
        )));
        register(new HatDefinition("compliment", "Compliment Hat", Material.COMMAND_BLOCK, HatRarity.COMMON, List.of(
                "You're amazing!", "I love your style!", "You smell great!"
        )));
        register(new HatDefinition("insult", "Insult Hat", Material.OBSIDIAN, HatRarity.COMMON, List.of(
                "You suck!", "I'm better!", "You smell bad!"
        )));
        register(new HatDefinition("robot", "Robot Hat", Material.IRON_HELMET, HatRarity.COMMON, List.of(
                "I'll be back.", "Get out.", "Hasta la vista."
        )));
        register(new HatDefinition("fox", "Fox Hat", Material.ORANGE_WOOL, HatRarity.COMMON, List.of(
                "RING A DING DING!", "SAY WHAT?", "I WANT TO KNOW!"
        )));
        register(new HatDefinition("mr_toad", "Mr. Toad Hat", Material.GREEN_TERRACOTTA, HatRarity.COMMON, List.of(
                "Ribbet.", "Croak.", "Ribbet-ribbet."
        )));
        register(new HatDefinition("detective", "Detective Hat", Material.LEATHER_HELMET, HatRarity.COMMON, List.of(
                "Elementary!", "Fetch my diary!", "Case closed!"
        )));
        register(new HatDefinition("teamplayer", "Teamplayer Hat", Material.LEATHER_HELMET, HatRarity.COMMON, List.of(
                "Way to go!", "Good job!", "You're getting there!"
        )));
        register(new HatDefinition("n00b", "n00b Hat", Material.COAL_ORE, HatRarity.COMMON, List.of(
                "OMG HAX!", "1v1 me!", "HACKER!"
        )));
        register(new HatDefinition("orange_hoodie", "Orange Hoodie Hat", Material.LEATHER_HELMET, HatRarity.COMMON, List.of(
                "Mmph mmf, mmph?", "Mmmpf mpph.", "Mmf mpf mommmppf!"
        )));
        register(new HatDefinition("wizard", "Wizard Hat", Material.LEATHER_HELMET, HatRarity.COMMON, List.of(
                "Hypixellamus!", "Alohypixel!", "Crucio!"
        )));
        register(new HatDefinition("canada", "Canada Hat", Material.LEATHER_HELMET, HatRarity.COMMON, List.of(
                "I'M SO SORRY.", "Syrup?", "What's that eh?"
        )));
        register(new HatDefinition("scotland", "Scotland Hat", Material.LAPIS_BLOCK, HatRarity.COMMON, List.of(
                "Haggis!", "Kilts!", "Whiskey!"
        )));
        register(new HatDefinition("potty_mouth", "Potty Mouth Hat", Material.DIRT, HatRarity.COMMON, List.of(
                "#&^$@(!.", "!*#&#(!&*.", "@*#!?#@*!"
        )));
        register(new HatDefinition("kim_jong_il", "Kim Jong-Il Hat", Material.LEATHER_HELMET, HatRarity.COMMON, List.of(
                "Why Harro there.", "I'm so ronernee.", "Cake? Cake!"
        )));
        register(new HatDefinition("gold_rush", "Gold Rush Hat", Material.GOLD_BLOCK, HatRarity.COMMON, List.of(
                "Budder.", "Budder.", "Mushroom."
        )));
        register(new HatDefinition("spartan", "Spartan Hat", Material.CHAINMAIL_HELMET, HatRarity.COMMON, List.of(
                "Madness? This is...", "Arena!", "Prepare for glory!"
        )));
        register(new HatDefinition("hipster", "Hipster Hat", Material.GRASS_BLOCK, HatRarity.COMMON, List.of(
                "The underground!", "Starbucks!", "Before it was cool!"
        )));
        register(new HatDefinition("try_hard", "Try Hard Hat", Material.IRON_HELMET, HatRarity.COMMON, List.of(
                "FITE ME.", "UWOTM8.", "UDED"
        )));
        register(new HatDefinition("librarian", "Librarian Hat", Material.BOOKSHELF, HatRarity.COMMON, List.of(
                "Shhh!", "Knowledge is power!", "Go read a book!"
        )));
        register(new HatDefinition("grumpy_cat", "Grumpy Cat Hat", Material.LEATHER_HELMET, HatRarity.COMMON,
                List.of("I hate games!", "I hate swords!", "I hate mondays!"), Color.fromRGB(0, 128, 0)));

        register(new HatDefinition("detective", "Detective Hat", Material.LEATHER_HELMET, HatRarity.COMMON,
                List.of("Elementary!", "Fetch my diary!", "Case closed!"), Color.fromRGB(128, 128, 128)));

        register(new HatDefinition("teamplayer", "Teamplayer Hat", Material.LEATHER_HELMET, HatRarity.COMMON,
                List.of("Way to go!", "Good job!", "You're getting there!"), Color.fromRGB(128, 0, 128)));

        register(new HatDefinition("orange_hoodie", "Orange Hoodie Hat", Material.LEATHER_HELMET, HatRarity.COMMON,
                List.of("Mmph mmf, mmph?", "Mmmpf mpph.", "Mmf mpf mommmppf!"), Color.fromRGB(255, 140, 0)));

        register(new HatDefinition("wizard", "Wizard Hat", Material.LEATHER_HELMET, HatRarity.COMMON,
                List.of("Hypixellamus!", "Alohypixel!", "Crucio!"), Color.fromRGB(20, 20, 20)));

        register(new HatDefinition("canada", "Canada Hat", Material.LEATHER_HELMET, HatRarity.COMMON,
                List.of("I'M SO SORRY.", "Syrup?", "What's that eh?"), Color.fromRGB(200, 0, 0)));

        register(new HatDefinition("kim_jong_il", "Kim Jong-Il Hat", Material.LEATHER_HELMET, HatRarity.COMMON,
                List.of("Why Harro there.", "I'm so ronernee.", "Cake? Cake!"), Color.fromRGB(255, 215, 0)));
    }

    private void registerRareHats() {
        register(new HatDefinition("doge", "Doge Hat", Material.PLAYER_HEAD, HatRarity.RARE, List.of(
                "such ouch.", "very damage.", "more pain."), null,
                UUID.fromString("f01fd694-40e7-4245-b687-b7803a9b7491")
        ));
        register(new HatDefinition("l33t", "l33t Hat", Material.REDSTONE_ORE, HatRarity.RARE, List.of(
                "GET ON MY LEVEL.", "UR WEAK SON.", "DO U EVEN LIFT"
        )));
        register(new HatDefinition("nukem", "Nukem Hat", Material.TNT, HatRarity.RARE, List.of(
                "Out of gum.", "Time to kick butt!", "Not quite."
        )));
        register(new HatDefinition("bebopvox", "BebopVox Hat", Material.PLAYER_HEAD, HatRarity.RARE, List.of(
                "Diamonds to you!", "Game on!", "So many to come stuff!"),null,UUID.fromString("6835219e-d8c1-44be-8780-16fe069f9725"
        )));
        register(new HatDefinition("kevinkool", "Kevinkool Hat", Material.PLAYER_HEAD, HatRarity.RARE, List.of(
                "More Fish!", "I'm kool!", "#KoolParty"),null,UUID.fromString("796589fa-87a1-4d48-8d40-f934b54322cc"
        )));
        register(HatDefinition.playerHead("thorlon", "Thorlon Hat", HatRarity.RARE,
                List.of("I am a god!", "I will smite you!", "Smited!"),
                UUID.fromString("d3be5ef3-db9b-48d3-a668-019bf3ce0495")));
        register(new HatDefinition("cactus", "Cactus Hat", Material.CACTUS, HatRarity.RARE, List.of(
                "Spikey!", "Oy-oy!", "Pyoing!"
        )));
        register(new HatDefinition("space", "Space Helmet", Material.IRON_BLOCK, HatRarity.RARE, List.of(
                "Ground control?", "Blast-off!", "Houston? We have a problem."
        )));
        register(new HatDefinition("polar_bear", "Polar Bear Hat", Material.SNOW_BLOCK, HatRarity.RARE, List.of(
                "Ice Ice Baby.", "#FROZEN", "Let it go!"
        )));
        register(new HatDefinition("anger", "Anger Hat", Material.RED_STAINED_GLASS, HatRarity.RARE, List.of(
                "RAAAAAWR.", "So angry!", "Smash!", "GRAAAAWR."
        )));
    }

    private void registerEpicHats() {
        register(new HatDefinition("helix", "Helix Hat", Material.PLAYER_HEAD, HatRarity.EPIC, List.of(
                "Praise Helix!", "up up", "anarchy", "a a start9"),null,UUID.fromString("7297edb1-aa5e-4265-aaf8-33f9cbc6c81b")
        ));
        register(new HatDefinition("sweg", "Sweg Hat", Material.GOLDEN_HELMET, HatRarity.EPIC, List.of(
                "Sweg.", "Swooty.", "Swiggity.", "Sweggity."
        )));
        register(new HatDefinition("diamond_helmet", "Diamond Helmet", Material.DIAMOND_HELMET, HatRarity.EPIC, List.of(
                "See you on the leaderboard!", "What's your rating?", "There can be only one!"
        )));
        register(new HatDefinition("bee_king", "Bee King Hat", Material.YELLOW_STAINED_GLASS, HatRarity.EPIC, List.of(
                "Bzzz.", "Honey, plz.", "Send in the drones", "and 1"
        )));
        register(new HatDefinition("edupa", "Edupa Hat", Material.PLAYER_HEAD, HatRarity.EPIC, List.of(
                "stop cry stop lie", "learn queue", "dargon under mine bed", "spend 50 beer money on 2 frog","minecraft more important than gf xD","hit me up with an chocolate","im rune like one saxobeat","im speak brazil"),null,UUID.fromString("747709c7-3449-4480-bfe0-58265d0e6509")
        ));
    }

    public void register(HatDefinition hat) {
        hatsById.put(hat.id(), hat);
        hatsByRarity.get(hat.rarity()).add(hat);
    }

    public HatDefinition get(String id) {
        return hatsById.get(id);
    }

    public List<HatDefinition> getByRarity(HatRarity rarity) {
        return hatsByRarity.get(rarity);
    }

    public Collection<HatDefinition> getAll() {
        return hatsById.values();
    }
}