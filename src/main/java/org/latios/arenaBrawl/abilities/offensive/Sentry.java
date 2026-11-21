package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.abilities.structures.SentryStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Deploys a small destructible turret that fires at the nearest visible enemy.
 */
public class Sentry implements Ability {

    private final double energyCost;
    private final double damage;
    private final double range;
    private final int fireIntervalTicks;
    private final int lifetimeSeconds;
    private final int hitsToDestroy;
    private final double maxPlacementRange;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final StructureManager structureManager;
    private final AbilityConfig config;

    public Sentry(EnergyManager energyManager, TeamManager teamManager, CombatService combatService,
                  StructureManager structureManager, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 55.0);
        this.damage = config.getDouble("damage", 22.0);
        this.range = config.getDouble("range", 12.0);
        this.fireIntervalTicks = Math.max(2, config.getInt("fire-interval-ticks", 10));
        this.lifetimeSeconds = Math.max(1, config.getInt("lifetime-seconds", 8));
        this.hitsToDestroy = Math.max(1, config.getInt("hits-to-destroy", 3));
        this.maxPlacementRange = config.getDouble("max-placement-range", 10.0);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.structureManager = structureManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Sentry";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) Math.ceil(maxPlacementRange));

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        Block placementBlock = targetBlock.getRelative(BlockFace.UP);
        if (!isReplaceable(placementBlock.getType())
                || !isReplaceable(placementBlock.getRelative(BlockFace.UP).getType())) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        SentryStructure structure = new SentryStructure(player, placementBlock.getLocation(), teamManager,
                combatService, config.getMatchManager(), damage, range, fireIntervalTicks,
                lifetimeSeconds, hitsToDestroy);
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

    @Override
    public String getDescription() {
        return "Deploys a destructible turret that shoots the nearest visible enemy for a few seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage per shot", (int) damage + " HP"),
                new AbilityStat("Range", (int) range + "m"),
                new AbilityStat("Lifetime", lifetimeSeconds + "s"),
                new AbilityStat("Melee hits to destroy", String.valueOf(hitsToDestroy)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
