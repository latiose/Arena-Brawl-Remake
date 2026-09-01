package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class SparkBolt implements Ability {

    private final int maxRange;
    private final long durationMillis;
    private final long cooldownMillis;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final EnergyModifierManager energyModifierManager;

    public SparkBolt(CooldownManager cooldownManager, TeamManager teamManager,
                     CombatUpgradeManager combatUpgradeManager, EnergyModifierManager energyModifierManager,
                     AbilityConfig config) {
        this.maxRange = config.getInt("max-range", 20);
        this.durationMillis = config.getLong("duration-ms", 5_000L);
        this.cooldownMillis = config.getLong("cooldown-ms", 30_000L);

        this.cost = new CooldownCost(cooldownManager, "sparkbolt", cooldownMillis, combatUpgradeManager);
        this.teamManager = teamManager;
        this.energyModifierManager = energyModifierManager;
    }

    @Override
    public String getName() { return "Spark Bolt"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        energyModifierManager.addModifier(target, "spark_bolt", 0.5, durationMillis);

        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);

        player.sendMessage(MessageUtils.positive() + "§3Your Spark Bolt hit §3" + target.getName() + " §3and halved their energy regen!");
        target.sendMessage(MessageUtils.negative() + "§3You were hit by §a" + player.getName() + "§3's Spark Bolt! Energy regen halved for " + (durationMillis / 1000L) + "s.");

        return true;
    }

    @Override
    public String getDescription() {
        return "Halves an enemy's energy regeneration for some time.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMillis / 1000L) + "s"),
                new AbilityStat("Range", maxRange + " blocks"),
                new AbilityStat("Duration", (durationMillis / 1000L) + "s")
        );
    }
}