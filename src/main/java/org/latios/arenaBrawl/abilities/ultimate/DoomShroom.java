package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.abilities.structures.Blueprints;
import org.latios.arenaBrawl.abilities.structures.DoomShroomStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * The dark mirror of the Tree of Life. It grows a giant mushroom that burns
 * every nearby enemy once per second and finally explodes for a large burst.
 * Enemies can chop it down with melee hits before it detonates.
 */
public class DoomShroom implements Ability {

    private final long chargeTimeMillis;
    private final double damagePerSecond;
    private final int damageIntervalTicks;
    private final double finalBurstDamage;
    private final int durationSeconds;
    private final int growthSeconds;
    private final double radius;
    private final int hitsToDestroy;
    private final double maxPlacementRange;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final StructureManager structureManager;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public DoomShroom(CooldownManager cooldownManager, UsageManager usageManager,
                      StructureManager structureManager, TeamManager teamManager,
                      CombatService combatService, AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.damagePerSecond = config.getDouble("damage-per-second", 30.0);
        this.damageIntervalTicks = Math.max(2, config.getInt("damage-interval-ticks", 10));
        this.finalBurstDamage = config.getDouble("final-burst-damage", 300.0);
        this.durationSeconds = config.getInt("duration-seconds", 6);
        this.growthSeconds = config.getInt("growth-seconds", 3);
        this.radius = config.getDouble("radius", 6.0);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 8);
        this.maxPlacementRange = config.getDouble("max-placement-range", 6.0);

        this.cost = new UltimateCost(cooldownManager, usageManager, "doomshroom");
        this.cooldownManager = cooldownManager;
        this.structureManager = structureManager;
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Doom Shroom";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "doomshroom", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Grows a giant doom shroom over a few seconds. It damages every nearby enemy in quick pulses "
                + "and finally explodes for a big burst. Enemies can chop it down with melee hits before "
                + "it detonates.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage per second", (int) damagePerSecond + " HP"),
                new AbilityStat("Pulse interval", (damageIntervalTicks / 20.0) + "s"),
                new AbilityStat("Final burst damage", (int) finalBurstDamage + " HP"),
                new AbilityStat("Duration", durationSeconds + "s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Melee hits to destroy", String.valueOf(hitsToDestroy)),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) Math.ceil(maxPlacementRange));

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        Block placementBlock = targetBlock.getRelative(BlockFace.UP);

        if (!isReplaceable(placementBlock.getType())) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        DoomShroomStructure structure = new DoomShroomStructure(
                player, placementBlock.getLocation(), Blueprints.doomShroomPhases(),
                teamManager, combatService, damagePerSecond, damageIntervalTicks, finalBurstDamage,
                durationSeconds, growthSeconds, radius, hitsToDestroy
        );
        structureManager.register(structure);

        Location center = placementBlock.getLocation().add(0.5, 0.5, 0.5);
        center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, 25, 0.6, 0.8, 0.6, 0.0);
        center.getWorld().spawnParticle(Particle.SMOKE, center, 20, 0.5, 0.6, 0.5, 0.02);
        center.getWorld().playSound(center, Sound.BLOCK_WOOD_PLACE, 1.2f, 0.7f);

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
