package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.*;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Barricade implements Ability {

    private final long cooldownMs;
    private final long durationMs;
    private final AbilityCost cost;
    private final StructureManager structureManager;

    public Barricade(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                     StructureManager structureManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.durationMs = config.getLong("duration-ms", 4000L);
        this.cost = new CooldownCost(cooldownManager, "barricade", cooldownMs, upgradeManager);
        this.structureManager = structureManager;
    }

    @Override
    public String getName() { return "Barricade"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Builds a climbable stone dome around you with a small gap at the top. Cannot be broken by melee attacks.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", formatSeconds(durationMs)),
                new AbilityStat("Height", "4 blocks"),
                new AbilityStat("Melee resistant", "Yes"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    private static String formatSeconds(long millis) {
        if (millis % 1000L == 0L) {
            return (millis / 1000L) + "s";
        }
        return String.format("%.1fs", millis / 1000.0);
    }

    @Override
    public boolean activate(Player player) {
        Location placementLocation = player.getLocation();
        StructureBlueprint blueprint = Blueprints.defaultBarricade();

        if (!BarricadeStructure.canBuild(placementLocation, blueprint)) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        BarricadeStructure structure = new BarricadeStructure(player, placementLocation, blueprint, durationMs);
        structureManager.register(structure);

        return true;
    }
}