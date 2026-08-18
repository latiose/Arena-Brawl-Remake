package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.HealthUtils;
import org.latios.arenaBrawl.team.TeamManager;

public class GroundSlam implements Ability {

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private static final double ENERGY_COST = 100.0;
    private final HealthUtils healthUtils;
    public GroundSlam(TeamManager teamManager, EnergyManager energyManager,HealthUtils healthUtils) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.healthUtils = healthUtils;
    }

    @Override
    public String getName() { return "Ground slam"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void activate(Player player) {
        for (Entity nearby : player.getNearbyEntities(4, 3, 4)) {
            if (nearby instanceof Player target && teamManager.isEnemy(player, target)) {
                healthUtils.damage(target, 250);
                target.setVelocity(target.getVelocity().setY(0.5));
            }
        }
    }
}