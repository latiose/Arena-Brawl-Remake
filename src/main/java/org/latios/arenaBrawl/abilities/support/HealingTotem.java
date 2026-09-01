package org.latios.arenaBrawl.abilities.support;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.HealingTotemStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class HealingTotem implements Ability {

    private final long cooldownMs;
    private final double healPerPulse;
    private final int pulses;
    private final double radius;
    private final int hitsToDestroy;

    private final AbilityCost cost;
    private final StructureManager structureManager;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public HealingTotem(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                        StructureManager structureManager, TeamManager teamManager,
                        PlayerHealthManager healthManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 40000L);
        this.healPerPulse = config.getDouble("heal-per-pulse", 300.0);
        this.pulses = config.getInt("pulses", 3);
        this.radius = config.getDouble("radius", 4.0);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 4);

        this.cost = new CooldownCost(cooldownManager, "healingtotem", cooldownMs, upgradeManager);
        this.structureManager = structureManager;
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Healing Totem"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Places a totem that heals you and nearby allies every 3 seconds, up to " + pulses + " times. "
                + "Enemies can destroy it with melee attacks.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal per pulse", (int) healPerPulse + " HP"),
                new AbilityStat("Pulses", String.valueOf(pulses)),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Melee hits to destroy", String.valueOf(hitsToDestroy)),
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
            player.sendMessage("§cCannot place Healing Totem! The space is obstructed.");
            return false;
        }
        var structure = new HealingTotemStructure(player, baseBlock.getLocation(), teamManager, healthManager);
        structureManager.register(structure);
        return true;
    }
}