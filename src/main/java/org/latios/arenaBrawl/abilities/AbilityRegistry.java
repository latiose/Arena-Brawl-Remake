package org.latios.arenaBrawl.abilities;

import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.abilities.offensive.*;
import org.latios.arenaBrawl.abilities.support.*;
import org.latios.arenaBrawl.abilities.ultimate.*;
import org.latios.arenaBrawl.abilities.utility.*;
import org.latios.arenaBrawl.abilities.utility.HazardLeapAbility;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AbilityRegistry {

    private final Map<AbilitySlot, Map<String, AbilityFactory>> registry = new EnumMap<>(AbilitySlot.class);
    private final Map<AbilitySlot, String> defaults = new EnumMap<>(AbilitySlot.class);
    private final Plugin plugin;
    public AbilityRegistry(Plugin plugin) {
        for (AbilitySlot slot : AbilitySlot.values()) {
            registry.put(slot, new LinkedHashMap<>());
        }
        this.plugin = plugin;
        registerDefaults();
    }

    private void registerDefaults() {
        register(AbilitySlot.OFFENSIVE, "fireball", deps -> new FireballAbility(deps.energyManager(),deps.teamManager(),deps.combatService()));
        register(AbilitySlot.OFFENSIVE, "snowball", deps -> new SnowballAbility(deps.energyManager(),deps.teamManager(),deps.combatService(),deps.debuffManager()));
        register(AbilitySlot.OFFENSIVE, "melonlauncher", deps -> new MelonLauncher(deps.energyManager(),deps.teamManager(),deps.combatService()));
        register(AbilitySlot.OFFENSIVE, "pumpkinlauncher", deps -> new PumpkinLauncher(deps.energyManager(),deps.teamManager(),deps.combatService()));
        register(AbilitySlot.UTILITY, "shadowstep", deps -> new ShadowStep(deps.cooldownManager(), deps.teamManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.UTILITY, "sugarrush", deps -> new SugarRush(deps.cooldownManager(),deps.combatUpgradeManager(),deps.debuffManager()));
        register(AbilitySlot.SUPPORT, "holywater", deps -> new HolyWater(deps.cooldownManager(),deps.teamManager(),deps.playerHealthManager(), deps.debuffManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.ULTIMATE, "shieldwall",
                deps -> new ShieldWall(plugin,deps.cooldownManager(), deps.usageManager(), deps.shieldManager()));
        register(AbilitySlot.OFFENSIVE, "groundslam", deps -> new GroundSlam(deps.teamManager(), deps.energyManager(),deps.combatService()));
        register(AbilitySlot.UTILITY, "polymorph",
                deps -> new Polymorph(deps.cooldownManager(), deps.teamManager(), deps.debuffManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.UTILITY, "swap",
                deps -> new Swap(deps.cooldownManager(), deps.debuffManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.SUPPORT, "boneshield",
                deps -> new BoneShield(deps.cooldownManager(), deps.orbitShieldManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.SUPPORT, "cactusshield",
                deps -> new CactusShield(deps.cooldownManager(), deps.orbitShieldManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.SUPPORT, "starshield",
                deps -> new StarShield(deps.cooldownManager(), deps.orbitShieldManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.SUPPORT, "spongeshield",
                deps -> new SpongeShield(deps.cooldownManager(), deps.orbitShieldManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.SUPPORT, "lifebond",
                deps -> new LifeBond(plugin,deps.cooldownManager(),deps.teamManager(),deps.playerHealthManager(), deps.combatUpgradeManager()));
        register(AbilitySlot.OFFENSIVE, "freezingbreath",
                deps -> new FreezingBreath(deps.energyManager(), deps.teamManager(), deps.combatService(),deps.debuffManager()));
        register(AbilitySlot.OFFENSIVE, "evilbreath",
                deps -> new EvilBreath(deps.energyManager(), deps.teamManager(), deps.combatService(),deps.debuffManager()));
        register(AbilitySlot.OFFENSIVE, "heavensbreath",
                deps -> new HeavensBreath(deps.energyManager(), deps.teamManager(), deps.combatService(),deps.playerHealthManager()));
        register(AbilitySlot.OFFENSIVE, "dragonsbreath",
                deps -> new DragonsBreath(plugin,deps.energyManager(), deps.teamManager(), deps.combatService(), deps.debuffManager()));
        register(AbilitySlot.OFFENSIVE, "ancientbreath",
                deps -> new AncientBreath(deps.energyManager(), deps.teamManager(), deps.combatService(),deps.debuffManager()));
        register(AbilitySlot.OFFENSIVE, "flamebreath",
                deps -> new FlameBreath(plugin,deps.energyManager(),deps.teamManager(),deps.combatService()));
        register(AbilitySlot.OFFENSIVE, "lightningstrike",
                deps -> new LightningStrike(deps.teamManager(),deps.energyManager(),deps.combatService(),deps.debuffManager()));
        register(AbilitySlot.ULTIMATE, "broodmother",
                deps -> new BroodMother(deps.cooldownManager(), deps.usageManager(), deps.broodMotherEntityManager(),deps.teamManager()));
        register(AbilitySlot.SUPPORT, "healingtotem",
                deps -> new HealingTotem(deps.cooldownManager(), deps.combatUpgradeManager(),
                        deps.structureManager(), deps.teamManager(), deps.playerHealthManager()));
        register(AbilitySlot.UTILITY, "barricade",
                deps -> new BarricadeAbility(deps.cooldownManager(), deps.combatUpgradeManager(), deps.structureManager()));
        register(AbilitySlot.UTILITY, "golemfall",
                deps -> new GolemFallAbility(deps.cooldownManager(), deps.combatUpgradeManager(),
                        deps.teamManager(), deps.demolitionService()));
        register(AbilitySlot.UTILITY, "bullcharge",
                deps -> new BullChargeAbility(deps.cooldownManager(), deps.combatUpgradeManager(),
                        deps.demolitionService(), deps.movementLockManager()));
        register(AbilitySlot.SUPPORT, "treeoflife",
                deps -> new TreeOfLifeAbility(deps.cooldownManager(), deps.combatUpgradeManager(),
                        deps.structureManager(), deps.teamManager(), deps.playerHealthManager()));
        register(AbilitySlot.UTILITY, "wallofvines",
                deps -> new WallOfVines(deps.cooldownManager(), deps.combatUpgradeManager(),
                        deps.structureManager(), deps.teamManager(),deps.debuffManager()));
        register(AbilitySlot.SUPPORT, "songofpower",
                deps -> new SongOfPowerAbility(deps.cooldownManager(), deps.combatUpgradeManager(),
                        deps.teamManager(), deps.songOfPowerManager(),deps.energyModifierManager(),deps.debuffManager()));
        register(AbilitySlot.UTILITY, "sparkbolt",
                deps -> new SparkBolt(deps.cooldownManager(),deps.teamManager(), deps.combatUpgradeManager()
                        ,deps.energyModifierManager()));
        register(AbilitySlot.UTILITY, "corruption",
                deps -> new Corruption(deps.cooldownManager(),deps.teamManager(), deps.combatUpgradeManager()
                        ,deps.debuffManager()));
        register(AbilitySlot.SUPPORT, "lifeleech",
                deps -> new LifeLeechAbility(deps.cooldownManager(), deps.combatUpgradeManager(), deps.lifeLeechManager()));
        register(AbilitySlot.OFFENSIVE, "consume",
                deps -> new ConsumeAbility(deps.energyManager(), deps.teamManager(), deps.combatService(), deps.playerHealthManager()));
        register(AbilitySlot.OFFENSIVE, "dash",
                deps -> new DashAbility(deps.energyManager(), deps.teamManager(), deps.combatService()));
        register(AbilitySlot.ULTIMATE, "absolutezero",
                deps -> new AbsoluteZeroAbility(deps.cooldownManager(), deps.usageManager(),
                        deps.teamManager(), deps.combatService(), deps.debuffManager()));
        register(AbilitySlot.OFFENSIVE, "rocketchicken",
                deps -> new RocketChickenAbility(deps.energyManager(), deps.teamManager(), deps.combatService()));
        register(AbilitySlot.OFFENSIVE, "laywaste",
                deps -> new LayWasteAbility(deps.energyManager(), deps.teamManager(), deps.combatService()));
        register(AbilitySlot.ULTIMATE, "nanoboost",
                deps -> new NanoBoostAbility(deps.cooldownManager(), deps.usageManager(), deps.teamManager(),
                        deps.shieldManager(), deps.damageBuffManager()));
        register(AbilitySlot.ULTIMATE, "divinejudgment",
                deps -> new DivineJudgmentAbility(deps.cooldownManager(), deps.usageManager(), deps.teamManager(),
                        deps.shieldManager(), deps.combatService()));
        register(AbilitySlot.ULTIMATE, "healingwind",
                deps -> new HealingWindAbility(deps.cooldownManager(), deps.usageManager(),
                        deps.teamManager(), deps.playerHealthManager()));
        register(AbilitySlot.OFFENSIVE, "particlebeam",
                deps -> new ParticleBeamAbility(deps.energyManager(), deps.teamManager(), deps.combatService()));
        register(AbilitySlot.OFFENSIVE, "cookieshotgun",
                deps -> new CookieShotgunAbility(deps.energyManager(), deps.teamManager(), deps.combatService()));
        register(AbilitySlot.SUPPORT, "etheralbody",
                deps -> new EtherealBodyAbility(deps.cooldownManager(),deps.etherealBodyManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.UTILITY, "violentleap",
                deps -> new HazardLeapAbility(deps.cooldownManager(),deps.teamManager(), deps.combatUpgradeManager()
                        ));
        register(AbilitySlot.UTILITY, "magneticImpulse",
                deps -> new MagneticImpulseAbility(deps.cooldownManager(), deps.debuffManager() ,deps.combatUpgradeManager()
                ));
        register(AbilitySlot.UTILITY, "darkpassage",
                deps -> new DarkPassage(plugin,deps.cooldownManager(), deps.teamManager() ,deps.combatUpgradeManager()
                ));
        register(AbilitySlot.ULTIMATE, "thebox",
                deps -> new TheBox(plugin,deps.cooldownManager(), deps.teamManager(),deps.combatService(),
                       deps.debuffManager(), deps.usageManager()));
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


    private AbilityDependencies previewDependencies; // set once, safe no-op dependencies for preview purposes

    public void setPreviewDependencies(AbilityDependencies deps) {
        this.previewDependencies = deps;
    }

    /**
     * Creates a throwaway instance of an ability purely to read its name/description for menus.
     * Never call activate() on the result — it may use dependencies that aren't tied to any real match.
     */
    public Ability createPreview(AbilitySlot slot, String id) {
        return create(slot, id, previewDependencies);
    }
}