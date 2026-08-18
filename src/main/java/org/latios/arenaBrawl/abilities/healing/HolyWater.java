package org.latios.arenaBrawl.abilities.healing;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.HealthUtils;
import org.latios.arenaBrawl.team.TeamManager;

public class HolyWater implements Ability {

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final HealthUtils healthManager;

    private static final double SELF_HEAL = 300;
    private static final double ALLY_HEAL = 50;

    public HolyWater(CooldownManager cooldownManager, TeamManager teamManager, HealthUtils healthManager) {
        this.cost = new CooldownCost(cooldownManager, "holywater", 30000);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Holy Water"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void activate(Player player) {
        Player closestAlly = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity nearby : player.getNearbyEntities(6, 4, 6)) {
            if (nearby instanceof Player nearbyPlayer && teamManager.isAlly(player, nearbyPlayer)) {
                double distance = nearbyPlayer.getLocation().distanceSquared(player.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestAlly = nearbyPlayer;
                }
            }
        }

        healthManager.heal(player, SELF_HEAL);
        if (closestAlly != null) {
            healthManager.heal(closestAlly, ALLY_HEAL);
        }

        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 10);
    }
}