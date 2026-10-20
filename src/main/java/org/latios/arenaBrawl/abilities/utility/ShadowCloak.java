package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Short invisibility with hidden armor and one empowered hit (melee or ability).
 */
public class ShadowCloak implements Ability {

    private final long cooldownMs;
    private final long durationMs;
    private final double damageBonus;
    private final int speedAmplifier;

    private final AbilityCost cost;
    private final ShadowCloakManager manager;

    public ShadowCloak(CooldownManager cooldownManager, ShadowCloakManager manager,
                       CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 28000L);
        this.durationMs = config.getLong("duration-ms", 3000L);
        this.damageBonus = config.getDouble("damage-bonus", 0.25);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);

        this.cost = new CooldownCost(cooldownManager, "shadowcloak", cooldownMs, combatUpgradeManager);
        this.manager = manager;
    }

    @Override
    public String getName() {
        return "Shadow Cloak";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        manager.activate(player, durationMs, 1.0 + damageBonus, speedAmplifier);
        return true;
    }

    @Override
    public String getDescription() {
        return "Become invisible for " + (durationMs / 1000L) +
                "s. Your armor and held items are hidden too. Your next attack or ability deals +" +
                (int) (damageBonus * 100) + "% damage and ends the cloak.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Next Hit", "+" + (int) (damageBonus * 100) + "%"),
                new AbilityStat("Speed", toRoman(speedAmplifier + 1) + " for " + (durationMs / 1000L) + "s")
        );
    }

    private String toRoman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }
}
