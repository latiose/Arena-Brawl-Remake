package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.Set;

public abstract class Breath implements Ability {

    protected static final double HIT_RADIUS = 2.5;
    protected static final double MAX_DISTANCE = 8.0;
    protected static final double STEP_SIZE = 0.2;
    protected static final double RADIUS_GROWTH = 0.22;

    protected final AbilityCost cost;
    protected final TeamManager teamManager;
    protected final CombatService combatService;
    protected final DebuffManager debuffManager;
    protected final Plugin plugin;

    public Breath(EnergyManager energyManager, double energyCost, TeamManager teamManager,
                         CombatService combatService, DebuffManager debuffManager) {
        this(null, energyManager, energyCost, teamManager, combatService, debuffManager);
    }

    public Breath(Plugin plugin, EnergyManager energyManager, double energyCost, TeamManager teamManager,
                         CombatService combatService, DebuffManager debuffManager) {
        this.plugin = plugin;
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        player.getWorld().playSound(player.getLocation(), getCastSound(), 1.0f, 1.0f);
        Location origin = player.getLocation().add(0, 1.4, 0);

        Vector axis = player.getEyeLocation().getDirection().setY(0);
        if (axis.lengthSquared() < 1e-6) {
            axis = player.getLocation().getDirection().setY(0);
        }
        axis.normalize();

        Vector right = new Vector(-axis.getZ(), 0, axis.getX()).normalize();
        Vector up = new Vector(0, 1, 0);

        Set<Player> hitPlayers = new HashSet<>();
        double currentAngle = 0.0;

        for (double distance = -1.0; distance <= MAX_DISTANCE; distance += STEP_SIZE) {
            double radius = Math.abs(distance) * RADIUS_GROWTH;

            double deltaTheta = 0.35 + (0.05 / (Math.abs(distance) + 0.1));
            currentAngle += deltaTheta;

            Location center = origin.clone().add(axis.clone().multiply(distance));

            Location strand1 = helixPoint(center, right, up, radius, currentAngle);
            Location strand2 = helixPoint(center, right, up, radius, currentAngle + Math.PI);

            spawnTrailParticles(strand1);
            spawnTrailParticles(strand2);

            checkHit(player, strand1, hitPlayers);
            checkHit(player, strand2, hitPlayers);
        }

        for (Player target : hitPlayers) {
            applyHitEffects(target);
            combatService.applyAbilityDamage(player, target, getDamage(), getName());
            applySpike(target);
        }

        return true;
    }

    protected Location helixPoint(Location center, Vector right, Vector up, double radius, double angle) {
        Vector offset = right.clone().multiply(Math.cos(angle) * radius)
                .add(up.clone().multiply(Math.sin(angle) * radius));
        return center.clone().add(offset);
    }

    protected void checkHit(Player caster, Location point, Set<Player> hitPlayers) {
        for (Player candidate : point.getWorld().getPlayers()) {
            if (candidate.equals(caster)) continue;
            if (!teamManager.isEnemy(caster, candidate)) continue;
            if (hitPlayers.contains(candidate)) continue;

            if (candidate.getLocation().distance(point) < HIT_RADIUS) {
                hitPlayers.add(candidate);
            }
        }
    }

    protected void applySpike(Player target) {
        Vector velocity = target.getVelocity();
        if (velocity.getY() < 0) {
            target.setVelocity(velocity.setY(velocity.getY() * 2));
        }
    }

    protected abstract Sound getCastSound();
    protected abstract double getDamage();
    protected abstract void spawnTrailParticles(Location point);
    protected abstract void applyHitEffects(Player target);
}