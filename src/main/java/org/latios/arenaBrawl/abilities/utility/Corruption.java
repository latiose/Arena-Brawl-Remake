package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Corruption implements Ability {

    private final double maxRange;
    private final long durationMillis;
    private final long cooldownMillis;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    public Corruption(CooldownManager cooldownManager, TeamManager teamManager,
                      CombatUpgradeManager combatUpgradeManager, DebuffManager debuffManager,
                      AbilityConfig config) {
        this.maxRange = config.getDouble("max-range", 20.0);
        this.durationMillis = config.getLong("duration-ms", 5000L);
        this.cooldownMillis = config.getLong("cooldown-ms", 35000L);

        this.cost = new CooldownCost(cooldownManager, "corruption", cooldownMillis, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Corruption"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        debuffManager.tryApply(player, target, DebuffType.ANTIHEAL, durationMillis);

        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 15);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 2.0f);

        player.sendMessage(MessageUtils.positive() + "§3You corrupted §3" + target.getName() + "§3!");
        target.sendMessage(MessageUtils.negative() + "§3You were hit by §a" + player.getName() + "§3's Corruption! Cannot heal for " + (durationMillis / 1000L) + "s.");

        return true;
    }

    @Override
    public String getDescription() {
        return "Prevents an enemy from healing for " + (durationMillis / 1000L) + " seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMillis / 1000L) + "s"),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Duration", (durationMillis / 1000L) + "s")
        );
    }
}