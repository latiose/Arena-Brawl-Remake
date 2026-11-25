package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.Blueprints;
import org.latios.arenaBrawl.abilities.structures.PhaseWallStructure;
import org.latios.arenaBrawl.abilities.structures.StructureBlueprint;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.structures.StructureUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Summons a temporary cage that only allies can cross. Enemies inside are trapped by the solid
 * walls, while allies phase through any of the four sides.
 */
public class PhaseWall implements Ability {

    private final long cooldownMs;
    private final long durationMs;
    private final int hitsToDestroy;
    private final double maxRange;

    private final AbilityCost cost;
    private final StructureManager structureManager;
    private final TeamManager teamManager;

    public PhaseWall(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                     StructureManager structureManager, TeamManager teamManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 35000L);
        this.durationMs = config.getLong("duration-ms", 5000L);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 6);
        this.maxRange = config.getDouble("max-range", 15.0);

        this.cost = new CooldownCost(cooldownManager, "phasewall", cooldownMs, upgradeManager);
        this.structureManager = structureManager;
        this.teamManager = teamManager;
    }

    @Override
    public String getName() {
        return "Phase Wall";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Summons a cage of spectral glass at a targeted location. Allies can phase through "
                + "its walls, while enemies are trapped inside and must break it with melee attacks.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Size", "5x5x3 cage"),
                new AbilityStat("Melee hits to destroy", String.valueOf(hitsToDestroy)),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) maxRange);

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        Location placementLocation = targetBlock.getRelative(BlockFace.UP).getLocation();
        StructureBlueprint blueprint = Blueprints.defaultPhaseCage();

        if (!StructureUtils.isSpaceClearForCage(placementLocation, blueprint)) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        PhaseWallStructure structure = new PhaseWallStructure(player, placementLocation, blueprint,
                teamManager, durationMs, hitsToDestroy);
        structureManager.register(structure);

        return true;
    }
}
