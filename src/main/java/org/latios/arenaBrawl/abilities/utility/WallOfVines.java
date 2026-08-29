package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.*;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class WallOfVines implements Ability {

    private final AbilityCost cost;
    private final StructureManager structureManager;
    private final DebuffManager debuffManager;
    private final TeamManager teamManager;

    public WallOfVines(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                       StructureManager structureManager, TeamManager teamManager, DebuffManager debuffManager) {
        this.cost = new CooldownCost(cooldownManager, "wallofvines", 30000, upgradeManager);
        this.structureManager = structureManager;
        this.debuffManager = debuffManager;
        this.teamManager = teamManager;
    }

    @Override
    public String getName() { return "Wall of vines"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Builds a climbable wall that immobilizes nearby enemies. "
                + "Cannot be broken by melee attacks.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", "5s"),
                new AbilityStat("Melee resistant", "Yes"),
                new AbilityStat("Immobilize duration", "2s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            player.sendMessage("§eSelect a valid block!");
            return false;
        }


        Location placementLocation = targetBlock.getRelative(BlockFace.UP).getLocation();
        StructureBlueprint blueprint = Blueprints.defaultWallOfVines();
        BlockFace facing = WallOfVinesStructure.getPlayerFacing(player);

    if(!StructureUtils.isSpaceClearForBlueprint(placementLocation, facing, blueprint)) {
            player.sendMessage("§eSelect a valid block!");
            return false;
        }

        WallOfVinesStructure structure = new WallOfVinesStructure(player, placementLocation, blueprint, teamManager, debuffManager);
        structureManager.register(structure);

        return true;
    }
}