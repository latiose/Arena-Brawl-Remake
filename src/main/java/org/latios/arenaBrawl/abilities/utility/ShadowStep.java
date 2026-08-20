package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.units.qual.Speed;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.team.TeamManager;

import java.lang.annotation.ElementType;

public class ShadowStep implements Ability {

    private final AbilityCost cost;
    private final TeamManager teamManager;

    private static final int MAX_RANGE = 24;
    private static final double BEHIND_DISTANCE = 1.5;

    public ShadowStep(CooldownManager cooldownManager, TeamManager teamManager) {
        this.cost = new CooldownCost(cooldownManager, "shadowstep", 30000);
        this.teamManager = teamManager;
    }

    @Override
    public String getName() { return "Shadow Step"; }

    @Override
    public AbilityCost getCost() { return cost; }



    @Override
    public boolean activate(Player player) {
        Player target = findEnemyTarget(player);

        if (target == null) {
            player.sendMessage("§cNo enemy in your crosshair.");
            return false;
        }

        Location teleportLocation = calculateBehindLocation(player, target);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.3, 0.5, 0.3);
        player.teleport(teleportLocation);
        player.getWorld().spawnParticle(Particle.SMOKE, teleportLocation, 20, 0.3, 0.5, 0.3);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,60,2));
        return true;
    }


    private Player findEnemyTarget(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                MAX_RANGE,
                0.3,
                entity -> entity instanceof Player p
                        && !p.equals(player)
                        && teamManager.isEnemy(player, p)
        );

        if (result == null || result.getHitEntity() == null) {
            return null;
        }

        return (Player) result.getHitEntity();
    }


    private Location calculateBehindLocation(Player player, Player target) {
        Vector targetDirection = target.getLocation().getDirection().normalize();
        Location behind = target.getLocation().clone().subtract(targetDirection.multiply(BEHIND_DISTANCE));
        behind.setY(target.getLocation().getY());

        if (!behind.getBlock().isPassable() || !behind.clone().add(0, 1, 0).getBlock().isPassable()) {
            behind = target.getLocation().clone();
        }

        Vector lookDirection = target.getLocation().toVector().subtract(behind.toVector()).normalize();
        behind.setDirection(lookDirection);
        return behind;
    }
}