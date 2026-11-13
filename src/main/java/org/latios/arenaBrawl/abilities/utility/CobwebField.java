package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.CobwebFieldStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Places a temporary 3x3 cobweb field on the selected block. Enemies are slowed by the
 * webs while the caster and their allies can cross it normally.
 */
public class CobwebField implements Ability {

    private final long cooldownMs;
    private final long durationMs;
    private final int hitsToDestroy;
    private final double maxRange;

    private final AbilityCost cost;
    private final StructureManager structureManager;
    private final TeamManager teamManager;

    public CobwebField(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                       StructureManager structureManager, TeamManager teamManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 35000L);
        this.durationMs = config.getLong("duration-ms", 5000L);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 5);
        this.maxRange = config.getDouble("max-range", 5.0);

        this.cost = new CooldownCost(cooldownManager, "cobwebfield", cooldownMs, upgradeManager);
        this.structureManager = structureManager;
        this.teamManager = teamManager;
    }

    @Override
    public String getName() {
        return "Cobweb Field";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Spreads a 3x3 cobweb field on the selected block. Enemies are slowed by the webs, "
                + "while you and your allies can cross it normally. Any web can be hit to break the field.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Size", "3x3 blocks"),
                new AbilityStat("Melee hits to destroy", String.valueOf(hitsToDestroy)),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) maxRange);

        if (targetBlock == null) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        // Grow the field in the air space above the selected block, or on the block itself
        // when it is already a passable block (tall grass, carpet, snow...).
        Block center = targetBlock.getType().isSolid()
                ? targetBlock.getRelative(BlockFace.UP)
                : targetBlock;

        CobwebFieldStructure structure = new CobwebFieldStructure(
                player, center.getLocation(), teamManager, durationMs, hitsToDestroy);

        if (structure.getPlacedBlocks().isEmpty()) {
            structure.remove();
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        structureManager.register(structure);

        Location effectCenter = center.getLocation().add(0.5, 0.5, 0.5);
        player.getWorld().playSound(effectCenter, Sound.BLOCK_COBWEB_PLACE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.POOF, effectCenter, 30, 1.5, 0.4, 1.5, 0.02);
        player.getWorld().spawnParticle(Particle.CRIT, effectCenter, 20, 1.5, 0.4, 1.5, 0.05);

        return true;
    }
}
