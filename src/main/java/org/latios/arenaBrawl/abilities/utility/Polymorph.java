package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;

public class Polymorph implements Ability {

    private static final long IMMOBILIZE_DURATION_MILLIS = 8_000;
    private static final double RANGE = 20.0;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    public Polymorph(CooldownManager cooldownManager, TeamManager teamManager, DebuffManager debuffManager) {
        this.cost = new CooldownCost(cooldownManager, "polymorph", 40000);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Polymorph"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void activate(Player player) {
        Player closestEnemy = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity nearby : player.getNearbyEntities(RANGE, RANGE, RANGE)) {
            if (nearby instanceof Player target && teamManager.isEnemy(player, target)) {
                double distance = target.getLocation().distanceSquared(player.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEnemy = target;
                }
            }
        }

        if (closestEnemy == null) {
            player.sendMessage("§cNo enemy in range!.");
            return;
        }

        boolean applied = debuffManager.tryApply(closestEnemy, DebuffType.IMMOBILIZE, IMMOBILIZE_DURATION_MILLIS);

        if (applied) {
            closestEnemy.getWorld().spawnParticle(Particle.CRIT, closestEnemy.getLocation(), 20);
            player.sendMessage("§aYou have turned " + closestEnemy.getName() + " into a sheep!");
            closestEnemy.sendMessage("§cYou have been turned into a sheep by " + player.getName() + "!");
        }
    }
}