package org.latios.arenaBrawl.abilities;

import org.latios.arenaBrawl.abilities.healing.HolyWater;
import org.latios.arenaBrawl.abilities.utility.Polymorph;
import org.latios.arenaBrawl.abilities.offensive.FireballAbility;
import org.latios.arenaBrawl.abilities.offensive.GroundSlam;
import org.latios.arenaBrawl.abilities.ultimate.ShieldWall;

import org.latios.arenaBrawl.abilities.utility.ShadowStep;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AbilityRegistry {

    private final Map<AbilitySlot, Map<String, AbilityFactory>> registry = new EnumMap<>(AbilitySlot.class);
    private final Map<AbilitySlot, String> defaults = new EnumMap<>(AbilitySlot.class);

    public AbilityRegistry() {
        for (AbilitySlot slot : AbilitySlot.values()) {
            registry.put(slot, new LinkedHashMap<>());
        }
        registerDefaults();
    }

    private void registerDefaults() {
        register(AbilitySlot.OFFENSIVE, "fireball", deps -> new FireballAbility(deps.energyManager()));
        register(AbilitySlot.UTILITY, "shadowstep", deps -> new ShadowStep(deps.cooldownManager(), deps.teamManager()));
        register(AbilitySlot.SUPPORT, "holywater", deps -> new HolyWater(deps.cooldownManager(),deps.teamManager(),deps.playerHealthManager(), deps.debuffManager()));
        register(AbilitySlot.ULTIMATE, "shieldwall",
                deps -> new ShieldWall(deps.cooldownManager(), deps.usageManager(), deps.shieldManager()));
        register(AbilitySlot.OFFENSIVE, "groundslam", deps -> new GroundSlam(deps.teamManager(), deps.energyManager(),deps.playerHealthManager()));
        register(AbilitySlot.UTILITY, "polymorph",
                deps -> new Polymorph(deps.cooldownManager(), deps.teamManager(), deps.debuffManager()));
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
}