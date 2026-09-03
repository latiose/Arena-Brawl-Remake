package org.latios.arenaBrawl.abilities;

import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.abilities.config.AbilityConfigManager;
import org.latios.arenaBrawl.abilities.offensive.*;
import org.latios.arenaBrawl.abilities.support.*;
import org.latios.arenaBrawl.abilities.ultimate.*;
import org.latios.arenaBrawl.abilities.utility.*;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AbilityRegistry {
    private final AbilityConfigManager configManager;
    private final Map<AbilitySlot, Map<String, AbilityFactory>> registry = new EnumMap<>(AbilitySlot.class);
    private final Map<AbilitySlot, String> defaults = new EnumMap<>(AbilitySlot.class);
    private final Plugin plugin;

    public AbilityRegistry(Plugin plugin, AbilityConfigManager abilityConfigManager) {
        for (AbilitySlot slot : AbilitySlot.values()) {
            registry.put(slot, new LinkedHashMap<>());
        }
        this.plugin = plugin;
        this.configManager = abilityConfigManager;
        registerDefaults();
    }

    private void registerDefaults() {
        // --- OFFENSIVE ---
        register(AbilitySlot.OFFENSIVE, "fireball",
                deps -> new FireballAbility(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("fireball")));
        register(AbilitySlot.OFFENSIVE, "snowball",
                deps -> new SnowballAbility(deps.energyManager(), deps.teamManager(), deps.combatService(), deps.debuffManager(), configManager.get("snowball")));
        register(AbilitySlot.OFFENSIVE, "melonlauncher",
                deps -> new MelonLauncher(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("melonlauncher")));
        register(AbilitySlot.OFFENSIVE, "pumpkinlauncher",
                deps -> new PumpkinLauncher(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("pumpkinlauncher")));
        register(AbilitySlot.OFFENSIVE, "groundslam",
                deps -> new GroundSlam(deps.teamManager(), deps.energyManager(), deps.combatService(), configManager.get("groundslam")));
        register(AbilitySlot.OFFENSIVE, "freezingbreath",
                deps -> new FreezingBreath(deps.energyManager(), deps.teamManager(), deps.combatService(), deps.debuffManager(), configManager.get("freezingbreath")));
        register(AbilitySlot.OFFENSIVE, "evilbreath",
                deps -> new EvilBreath(deps.energyManager(), deps.teamManager(), deps.combatService(), deps.debuffManager(), configManager.get("evilbreath")));
        register(AbilitySlot.OFFENSIVE, "heavensbreath",
                deps -> new HeavensBreath(deps.energyManager(), deps.teamManager(), deps.combatService(), deps.playerHealthManager(), configManager.get("heavensbreath")));
        register(AbilitySlot.OFFENSIVE, "dragonsbreath",
                deps -> new DragonsBreath(plugin, deps.energyManager(), deps.teamManager(), deps.combatService(), deps.debuffManager(), configManager.get("dragonsbreath")));
        register(AbilitySlot.OFFENSIVE, "ancientbreath",
                deps -> new AncientBreath(deps.energyManager(), deps.teamManager(), deps.combatService(), deps.debuffManager(), configManager.get("ancientbreath")));
        register(AbilitySlot.OFFENSIVE, "flamebreath",
                deps -> new FlameBreath(plugin, deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("flamebreath")));
        register(AbilitySlot.OFFENSIVE, "lightningstrike",
                deps -> new LightningStrike(deps.teamManager(), deps.energyManager(), deps.combatService(), deps.debuffManager(), configManager.get("lightningstrike")));
        register(AbilitySlot.OFFENSIVE, "consume",
                deps -> new Consume(deps.energyManager(), deps.teamManager(), deps.combatService(), deps.playerHealthManager(), configManager.get("consume")));
        register(AbilitySlot.OFFENSIVE, "dash",
                deps -> new DashAbility(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("dash")));
        register(AbilitySlot.OFFENSIVE, "rocketchicken",
                deps -> new RocketChicken(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("rocketchicken")));
        register(AbilitySlot.OFFENSIVE, "mysticshot",
                deps -> new MysticShot(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("mysticshot")));
        register(AbilitySlot.OFFENSIVE, "laywaste",
                deps -> new LayWaste(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("laywaste")));
        register(AbilitySlot.OFFENSIVE, "particlebeam",
                deps -> new ParticleBeam(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("particlebeam")));
        register(AbilitySlot.OFFENSIVE, "BurstFire",
                deps -> new BurstFire(plugin, deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("BurstFire")));
        register(AbilitySlot.OFFENSIVE, "cookieshotgun",
                deps -> new CookieShotgun(deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("cookieshotgun")));
        register(AbilitySlot.OFFENSIVE, "spikegrenade",
                deps -> new SpikeGrenade(plugin, deps.energyManager(), deps.teamManager(), deps.combatService(), configManager.get("spikegrenade")));

        // --- UTILITY ---
        register(AbilitySlot.UTILITY, "shadowstep",
                deps -> new ShadowStep(deps.cooldownManager(), deps.teamManager(), deps.combatUpgradeManager(), configManager.get("shadowstep")));
        register(AbilitySlot.UTILITY, "salmonform",
                deps -> new SalmonForm(deps.cooldownManager(), deps.combatUpgradeManager(), configManager.get("salmon_form")));
        register(AbilitySlot.UTILITY, "sparkbolt",
                deps -> new SparkBolt(deps.cooldownManager(), deps.teamManager(), deps.combatUpgradeManager(), deps.energyModifierManager(), configManager.get("sparkbolt")));
        register(AbilitySlot.UTILITY, "sugarrush",
                deps -> new SugarRush(deps.cooldownManager(), deps.combatUpgradeManager(), deps.debuffManager(), configManager.get("sugarrush")));
        register(AbilitySlot.UTILITY, "swap",
                deps -> new Swap(deps.cooldownManager(), deps.debuffManager(), deps.combatUpgradeManager(), configManager.get("swap")));
        register(AbilitySlot.UTILITY, "violentleap",
                deps -> new ViolentLeap(deps.cooldownManager(), deps.teamManager(), deps.combatUpgradeManager(), configManager.get("violentleap")));
        register(AbilitySlot.UTILITY, "wallofvines",
                deps -> new WallOfVines(deps.cooldownManager(), deps.combatUpgradeManager(), deps.structureManager(), deps.teamManager(), deps.debuffManager(), configManager.get("wallofvines")));
        register(AbilitySlot.UTILITY, "polymorph",
                deps -> new Polymorph(deps.cooldownManager(), deps.teamManager(), deps.debuffManager(), deps.combatUpgradeManager(), configManager.get("polymorph")));
        register(AbilitySlot.UTILITY, "barricade",
                deps -> new Barricade(deps.cooldownManager(), deps.combatUpgradeManager(), deps.structureManager(), configManager.get("barricade")));
        register(AbilitySlot.UTILITY, "golemfall",
                deps -> new GolemFall(plugin, deps.cooldownManager(), deps.combatUpgradeManager(), deps.teamManager(), deps.demolitionService(), configManager.get("golemfall")));
        register(AbilitySlot.UTILITY, "bullcharge",
                deps -> new BullCharge(plugin, deps.cooldownManager(), deps.combatUpgradeManager(), deps.demolitionService(), deps.movementLockManager(), configManager.get("bullcharge")));
        register(AbilitySlot.UTILITY, "corruption",
                deps -> new Corruption(deps.cooldownManager(), deps.teamManager(), deps.combatUpgradeManager(), deps.debuffManager(), configManager.get("corruption")));
        register(AbilitySlot.UTILITY, "magneticImpulse",
                deps -> new MagneticImpulse(plugin, deps.cooldownManager(), deps.debuffManager(), deps.combatUpgradeManager(), configManager.get("magneticImpulse")));
        register(AbilitySlot.UTILITY, "RocketGrab",
                deps -> new RocketGrab(plugin, deps.cooldownManager(), deps.teamManager(), deps.combatUpgradeManager(), configManager.get("RocketGrab")));
        register(AbilitySlot.UTILITY, "darkpassage",
                deps -> new DarkPassage(plugin, deps.cooldownManager(), deps.teamManager(), deps.combatUpgradeManager(), configManager.get("darkpassage")));
        register(AbilitySlot.UTILITY, "dodgeroll",
                deps -> new DodgeRoll(plugin, deps.cooldownManager(), deps.combatUpgradeManager(), deps.shieldManager(), configManager.get("dodgeroll")));

        // --- SUPPORT ---
        register(AbilitySlot.SUPPORT, "holywater",
                deps -> new HolyWater(deps.cooldownManager(), deps.teamManager(), deps.playerHealthManager(), deps.debuffManager(), deps.combatUpgradeManager(), configManager.get("holywater")));
        register(AbilitySlot.SUPPORT, "boneshield",
                deps -> new BoneShield(deps.cooldownManager(), deps.orbitShieldManager(), deps.combatUpgradeManager(), configManager.get("boneshield")));
        register(AbilitySlot.SUPPORT, "cactusshield",
                deps -> new CactusShield(deps.cooldownManager(), deps.orbitShieldManager(), deps.combatUpgradeManager(), configManager.get("cactusshield")));
        register(AbilitySlot.SUPPORT, "starshield",
                deps -> new StarShield(deps.cooldownManager(), deps.orbitShieldManager(), deps.combatUpgradeManager(), configManager.get("starshield")));
        register(AbilitySlot.SUPPORT, "spongeshield",
                deps -> new SpongeShield(deps.cooldownManager(), deps.orbitShieldManager(), deps.combatUpgradeManager(), configManager.get("spongeshield")));
        register(AbilitySlot.SUPPORT, "lifebond",
                deps -> new LifeBond(plugin, deps.cooldownManager(), deps.teamManager(), deps.playerHealthManager(), deps.combatUpgradeManager(), configManager.get("lifebond")));
        register(AbilitySlot.SUPPORT, "healingtotem",
                deps -> new HealingTotem(deps.cooldownManager(), deps.combatUpgradeManager(), deps.structureManager(), deps.teamManager(), deps.playerHealthManager(), configManager.get("healingtotem")));
        register(AbilitySlot.SUPPORT, "treeoflife",
                deps -> new TreeOfLife(deps.cooldownManager(), deps.combatUpgradeManager(), deps.structureManager(), deps.teamManager(), deps.playerHealthManager(), configManager.get("treeoflife")));
        register(AbilitySlot.SUPPORT, "songofpower",
                deps -> new SongOfPower(deps.cooldownManager(), deps.combatUpgradeManager(), deps.teamManager(), deps.songOfPowerManager(), deps.energyModifierManager(), deps.debuffManager(), configManager.get("songofpower")));
        register(AbilitySlot.SUPPORT, "lifeleech",
                deps -> new LifeLeech(deps.cooldownManager(), deps.combatUpgradeManager(), deps.lifeLeechManager(), configManager.get("lifeleech")));
        register(AbilitySlot.SUPPORT, "etheralbody",
                deps -> new EtherealBody(deps.cooldownManager(), deps.etherealBodyManager(), deps.combatUpgradeManager(), configManager.get("etheralbody")));
        register(AbilitySlot.SUPPORT, "healingrain",
                deps -> new HealingRain(plugin, deps.cooldownManager(), deps.teamManager(), deps.playerHealthManager(), deps.combatUpgradeManager(), configManager.get("healingrain")));
        register(AbilitySlot.SUPPORT, "suzu",
                deps -> new Suzu(plugin, deps.cooldownManager(), deps.teamManager(), deps.playerHealthManager(), deps.shieldManager(), deps.combatUpgradeManager(), configManager.get("suzu")));
        register(AbilitySlot.SUPPORT, "discordorb",
                deps -> new DiscordOrb(plugin, deps.cooldownManager(), deps.teamManager(), deps.combatUpgradeManager(), deps.damageVulnerabilityManager(), configManager.get("discordorb")));
        register(AbilitySlot.SUPPORT, "healingbeam",
                deps -> new HealingBeam(deps.cooldownManager(), deps.teamManager(), deps.playerHealthManager(), deps.combatUpgradeManager(), configManager.get("healingbeam")));
        register(AbilitySlot.SUPPORT, "berserkerrage",
                deps -> new BerserkerRage(deps.playerHealthManager(), deps.damageBuffManager(), deps.cooldownManager(), deps.combatUpgradeManager(), configManager.get("berserkerrage")));

        // --- ULTIMATE ---
        register(AbilitySlot.ULTIMATE, "shieldwall",
                deps -> new ShieldWall(plugin, deps.cooldownManager(), deps.usageManager(), deps.shieldManager(), configManager.get("shieldwall")));
        register(AbilitySlot.ULTIMATE, "broodmother",
                deps -> new BroodMother(deps.cooldownManager(), deps.usageManager(), deps.broodMotherEntityManager(), deps.teamManager(), configManager.get("broodmother")));
        register(AbilitySlot.ULTIMATE, "absolutezero",
                deps -> new AbsoluteZeroAbility(deps.cooldownManager(), deps.usageManager(), deps.teamManager(), deps.combatService(), deps.debuffManager(), configManager.get("absolutezero")));
        register(AbilitySlot.ULTIMATE, "nanoboost",
                deps -> new NanoBoost(plugin, deps.cooldownManager(), deps.usageManager(), deps.teamManager(), deps.shieldManager(), deps.damageBuffManager(), configManager.get("nanoboost")));
        register(AbilitySlot.ULTIMATE, "divinejudgment",
                deps -> new DivineJudgment(deps.cooldownManager(), deps.usageManager(), deps.teamManager(), deps.shieldManager(), deps.combatService(), configManager.get("divinejudgment")));
        register(AbilitySlot.ULTIMATE, "healingwind",
                deps -> new HealingWind(deps.cooldownManager(), deps.usageManager(), deps.teamManager(), deps.playerHealthManager(), configManager.get("healingwind")));
        register(AbilitySlot.ULTIMATE, "thebox",
                deps -> new TheBox(plugin, deps.cooldownManager(), deps.teamManager(), deps.combatService(), deps.debuffManager(), deps.usageManager(), configManager.get("thebox")));
        register(AbilitySlot.ULTIMATE, "rewind",
                deps -> new Rewind(plugin, deps.cooldownManager(), deps.teamManager(), deps.usageManager(), configManager.get("rewind")));
        register(AbilitySlot.ULTIMATE, "staticfield",
                deps -> new StaticField(deps.cooldownManager(), deps.teamManager(), deps.combatService(), deps.debuffManager(), deps.usageManager(), configManager.get("staticfield")));
        register(AbilitySlot.ULTIMATE, "myriadtruths",
                deps -> new MyriadTruths(plugin, deps.cooldownManager(), deps.usageManager(), deps.teamManager(), deps.damageVulnerabilityManager(), configManager.get("myriadtruths")));
        register(AbilitySlot.ULTIMATE, "arenadomain",
                deps -> new ArenaDomain(plugin, deps.cooldownManager(), deps.usageManager(), deps.teamManager(), configManager.get("arenadomain")));
        register(AbilitySlot.ULTIMATE, "ninjadash",
                deps -> new NinjaDash(plugin, deps.cooldownManager(), deps.usageManager(), deps.teamManager(), deps.combatService(), configManager.get("ninjadash")));

        // --- DEFAULTS ---
        defaults.put(AbilitySlot.OFFENSIVE, "fireball");
        defaults.put(AbilitySlot.UTILITY, "shadowstep");
        defaults.put(AbilitySlot.SUPPORT, "holywater");
        defaults.put(AbilitySlot.ULTIMATE, "shieldwall");
    }

    public void register(AbilitySlot slot, String id, AbilityFactory factory) {
        registry.get(slot).put(id, factory);
    }

    public Ability create(AbilitySlot slot, String id, AbilityDependencies deps) {
        AbilityFactory factory = registry.get(slot).get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Ability with id '" + id + "' doesn't exist");
        }
        return factory.create(deps);
    }

    public String getDefault(AbilitySlot slot) {
        return defaults.get(slot);
    }

    public Set<String> getAvailableIds(AbilitySlot slot) {
        return registry.get(slot).keySet();
    }

    private AbilityDependencies previewDependencies;

    public void setPreviewDependencies(AbilityDependencies deps) {
        this.previewDependencies = deps;
    }

    public Ability createPreview(AbilitySlot slot, String id) {
        return create(slot, id, previewDependencies);
    }

    public Ability get(String chosenId) {
        if (chosenId == null) return null;

        for (AbilitySlot slot : registry.keySet()) {
            Map<String, AbilityFactory> slotAbilities = registry.get(slot);
            if (slotAbilities.containsKey(chosenId)) {
                return createPreview(slot, chosenId);
            }
        }
        return null;
    }
}