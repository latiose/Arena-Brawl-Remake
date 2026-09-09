package org.latios.arenaBrawl.abilities.support;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.MagicTableStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class MagicTable implements Ability {

    private final long cooldownMs;
    private final double healAmount;
    private final double radius;
    private final int hitsToDestroy;
    private final long delaySeconds;

    private final AbilityCost cost;
    private final StructureManager structureManager;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public MagicTable(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                      StructureManager structureManager, TeamManager teamManager,
                      PlayerHealthManager healthManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 35000L);
        this.healAmount = config.getDouble("heal-amount", 1000.0);
        this.radius = config.getDouble("radius", 5.0);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 4);
        this.delaySeconds = config.getLong("delay-seconds", 9L);

        this.cost = new CooldownCost(cooldownManager, "magictable", cooldownMs, upgradeManager);
        this.structureManager = structureManager;
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Magic Table"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Places a magical enchantment table with a candle. After " + delaySeconds + " seconds, "
                + "it bursts to heal you and nearby allies for " + (int) healAmount + " HP. "
                + "Enemies can destroy it with " + hitsToDestroy + " melee hits.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", (int) healAmount + " HP"),
                new AbilityStat("Delay", delaySeconds + "s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Hits to destroy", String.valueOf(hitsToDestroy)),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact(7);

        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            player.sendMessage("§cYou must aim at a solid block on the ground!");
            return false;
        }

        Block baseBlock = targetBlock.getRelative(0, 1, 0);
        Block topBlock = baseBlock.getRelative(0, 1, 0);

        if (baseBlock.getType().isSolid() || topBlock.getType().isSolid()) {
            player.sendMessage("§cCannot place Magic Table! The space is obstructed.");
            return false;
        }

        var structure = new MagicTableStructure(
                player,
                baseBlock.getLocation(),
                teamManager,
                healthManager
        );
        structureManager.register(structure);
        return true;
    }
}