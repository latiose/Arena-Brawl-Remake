package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Material;
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
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.structures.TreeOfLifeStructure;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class TreeOfLife implements Ability {

    private final long cooldownMs;
    private final double healPerSecond;
    private final double finalBurstHeal;
    private final int durationSeconds;
    private final double radius;
    private final int hitsToDestroy;
    private final double maxPlacementRange;

    private final AbilityCost cost;
    private final StructureManager structureManager;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public TreeOfLife(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                      StructureManager structureManager, TeamManager teamManager,
                      PlayerHealthManager healthManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 35000L);
        this.healPerSecond = config.getDouble("heal-per-second", 50.0);
        this.finalBurstHeal = config.getDouble("final-burst-heal", 400.0);
        this.durationSeconds = config.getInt("duration-seconds", 7);
        this.radius = config.getDouble("radius", 5.0);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 8);
        this.maxPlacementRange = config.getDouble("max-placement-range", 5.0);

        this.cost = new CooldownCost(cooldownManager, "treeoflife", cooldownMs, upgradeManager);
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
        return "Grows a tree over 5 seconds that heals nearby allies for a reduced amount of HP every second. "
                + "After some seconds, it bursts for a big burst. Can be chopped down with melee attacks.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal per second", (int) healPerSecond + " HP"),
                new AbilityStat("Final burst heal", (int) finalBurstHeal + " HP"),
                new AbilityStat("Duration", durationSeconds + "s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Melee hits to destroy", String.valueOf(hitsToDestroy)),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) maxPlacementRange);

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            player.sendMessage("§eSelect a valid block!");
            return false;
        }

        Block placementBlock = targetBlock.getRelative(BlockFace.UP);

        if (!isReplaceable(placementBlock.getType())) {
            player.sendMessage("§eSelect a valid block!");
            return false;
        }

        var structure = new TreeOfLifeStructure(
                player, placementBlock.getLocation(), Blueprints.growthPhases(), teamManager, healthManager
        );
        structureManager.register(structure);
        return true;
    }

    private boolean isReplaceable(Material material) {
        return material.isAir()
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.SNOW
                || material.name().endsWith("_CARPET");
    }
}