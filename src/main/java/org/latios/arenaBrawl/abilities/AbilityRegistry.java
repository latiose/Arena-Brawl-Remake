package org.latios.arenaBrawl.abilities;

import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.abilities.offensive.*;
import org.latios.arenaBrawl.abilities.support.HealingTotem;
import org.latios.arenaBrawl.abilities.support.StarShield;
import org.latios.arenaBrawl.abilities.support.BoneShield;
import org.latios.arenaBrawl.abilities.support.HolyWater;
import org.latios.arenaBrawl.abilities.ultimate.BroodMother;
import org.latios.arenaBrawl.abilities.utility.*;
import org.latios.arenaBrawl.abilities.ultimate.ShieldWall;

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
        register(AbilitySlot.OFFENSIVE, "melonlauncher", deps -> new MelonLauncher(deps.energyManager(),deps.teamManager(),deps.combatService()));
        register(AbilitySlot.OFFENSIVE, "pumpkinlauncher", deps -> new PumpkinLauncher(deps.energyManager(),deps.teamManager(),deps.combatService()));
        register(AbilitySlot.UTILITY, "shadowstep", deps -> new ShadowStep(deps.cooldownManager(), deps.teamManager(),deps.combatUpgradeManager()));
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
        register(AbilitySlot.SUPPORT, "starshield",
                deps -> new StarShield(deps.cooldownManager(), deps.orbitShieldManager(),deps.combatUpgradeManager()));
        register(AbilitySlot.OFFENSIVE, "freezingbreath",
                deps -> new FreezingBreath(deps.energyManager(), deps.teamManager(), deps.combatService(),deps.debuffManager()));
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
        register(AbilitySlot.UTILITY, "bullcharge",
                deps -> new BullChargeAbility(deps.cooldownManager(), deps.combatUpgradeManager(),
                        deps.structureManager(), deps.movementLockManager(),deps.teamManager()));
        register(AbilitySlot.UTILITY, "wallofvines",
                deps -> new WallOfVines(deps.cooldownManager(), deps.combatUpgradeManager(),
                        deps.structureManager(), deps.teamManager(),deps.debuffManager()));
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