// abilities/support/TreeOfLifeAbility.java
package org.latios.arenaBrawl.abilities.support;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.Blueprints;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.structures.TreeOfLifeStructure;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class TreeOfLifeAbility implements Ability {

    private final AbilityCost cost;
    private final StructureManager structureManager;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public TreeOfLifeAbility(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                             StructureManager structureManager, TeamManager teamManager,
                             PlayerHealthManager healthManager) {
        this.cost = new CooldownCost(cooldownManager, "treeoflife", 35000, upgradeManager);
        this.structureManager = structureManager;
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Tree of Life"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Grows a tree over 5 seconds that heals nearby allies for 50 HP every second. "
                + "After 7 seconds total, it bursts for a final 400 HP heal. Can be chopped down with 8 hits.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal per second", "50 HP"),
                new AbilityStat("Final burst heal", "400 HP"),
                new AbilityStat("Duration", "7s"),
                new AbilityStat("Radius", "5 blocks"),
                new AbilityStat("Melee hits to destroy", "8")
        );
    }

    @Override
    public boolean activate(Player player) {
        var structure = new TreeOfLifeStructure(
                player, player.getLocation(), Blueprints.growthPhases(), teamManager, healthManager
        );
        structureManager.register(structure);
        return true;
    }
}