package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class SparkBolt implements Ability {

    private static final int MAX_RANGE = 20;
    private static final long DURATION_MILLIS = 5_000;
    private static final long COOLDOWN_MILLIS = 30_000;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final EnergyModifierManager energyModifierManager;

    public SparkBolt(CooldownManager cooldownManager, TeamManager teamManager,
                     CombatUpgradeManager combatUpgradeManager, EnergyModifierManager energyModifierManager) {
        this.cost = new CooldownCost(cooldownManager, "sparkbolt", COOLDOWN_MILLIS, combatUpgradeManager);
        this.teamManager = teamManager;
        this.energyModifierManager = energyModifierManager;
    }

    @Override
    public String getName() { return "Spark Bolt"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, MAX_RANGE);

        if (target == null) {
            player.sendMessage("§eThere is no valid player within range!");
            return false;
        }

        energyModifierManager.addModifier(target, "spark_bolt", 0.5, DURATION_MILLIS);

        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);

        player.sendMessage(MessageUtils.positive() + "§3Your Spark Bolt hit §3" + target.getName() + " §3and halved their energy regen!");
        target.sendMessage(MessageUtils.negative() + "§3You were hit by §a" + player.getName() + "§3's Spark Bolt! Energy regen halved for 5s.");

        return true;
    }

    @Override
    public String getDescription() {
        return "Halves an enemy's energy regeneration for 5 seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (COOLDOWN_MILLIS / 1000) + "s"),
                new AbilityStat("Range", MAX_RANGE + " blocks"),
                new AbilityStat("Duration", "5s")
        );
    }
}