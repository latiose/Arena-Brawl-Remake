package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

public class HolyWater implements Ability {

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final DebuffManager debuffManager;
    private static final double SELF_HEAL = 300;
    private static final double ALLY_HEAL = 50;

    public HolyWater(CooldownManager cooldownManager, TeamManager teamManager, PlayerHealthManager healthManager,DebuffManager debuffManager) {
        this.cost = new CooldownCost(cooldownManager, "holywater", 30000);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Holy Water"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
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
        debuffManager.clear(player);
        player.sendMessage("§aYour holy water healed you for " + SELF_HEAL + " health!");
        if (closestAlly != null) {
            healthManager.heal(closestAlly, ALLY_HEAL);
            debuffManager.clear(closestAlly);
            player.sendMessage("§aYour holy water healed " + closestAlly.getName() + " for " + ALLY_HEAL + " health!");
            closestAlly.sendMessage("§a"+ player.getName() + "'s holy water healed your for " + ALLY_HEAL + " health!");
        }

        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 50);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f);
        return true;
    }
}