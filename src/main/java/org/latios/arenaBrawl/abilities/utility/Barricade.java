// abilities/utility/BarricadeAbility.java
package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.*;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Barricade implements Ability {

    private final AbilityCost cost;
    private final StructureManager structureManager;

    public Barricade(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                     StructureManager structureManager) {
        this.cost = new CooldownCost(cooldownManager, "barricade", 30000, upgradeManager);
        this.structureManager = structureManager;
    }

    @Override
    public String getName() { return "Barricade"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Builds a climbable wall around you with a gap to slip through at the top. "
                + "Cannot be broken by melee attacks";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", "5s"),
                new AbilityStat("Height", "3 blocks"),
                new AbilityStat("Melee resistant", "Yes")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location placementLocation = player.getLocation();
        StructureBlueprint blueprint = Blueprints.defaultBarricade();

        if (!BarricadeStructure.canBuild(placementLocation, blueprint)) {
            player.sendMessage("§cCannot place Barricade!");
            return false;
        }

        BarricadeStructure structure = new BarricadeStructure(player, placementLocation, blueprint);
        structureManager.register(structure);

        return true;
    }
}