
package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;

import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.*;

public class FreezingBreath implements Ability {

    private static final double DAMAGE = 220.0;
    private static final double ENERGY_COST = 80.0;
    private static final long SLOW_DURATION_TICKS = 2_000;
    private static final double HIT_RADIUS = 2.5;
    private static final double MAX_DISTANCE = 8.0;
    private static final double STEP_SIZE = 0.2;

    // Double helix shape: radius grows linearly with distance (cone), two strands
    // 180 degrees apart in rotation phase ("upright" and "upside down").
    private static final double RADIUS_GROWTH = 0.22;   // radius per block travelled


    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;

    public FreezingBreath(EnergyManager energyManager, TeamManager teamManager, CombatService combatService,DebuffManager debuffManager) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Freezing Breath"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
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
            applySlow(target);
            combatService.applyAbilityDamage(player, target, DAMAGE, getName());
            applySpike(target);
        }

        return true;
    }

    private Location helixPoint(Location center, Vector right, Vector up, double radius, double angle) {
        Vector offset = right.clone().multiply(Math.cos(angle) * radius)
                .add(up.clone().multiply(Math.sin(angle) * radius));
        return center.clone().add(offset);
    }

    private void spawnTrailParticles(Location point) {
       point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 3, 0, 0, 0, 0);
        point.getWorld().spawnParticle(Particle.DRIPPING_WATER, point, 1, 0, 0, 0, 0);

    }

    private void checkHit(Player caster, Location point, Set<Player> hitPlayers) {
        for (Player candidate : point.getWorld().getPlayers()) {
            if (candidate.equals(caster)) continue;
            if (!teamManager.isEnemy(caster, candidate)) continue;
            if (hitPlayers.contains(candidate)) continue;

            if (candidate.getLocation().distance(point) < HIT_RADIUS) {
                hitPlayers.add(candidate);
            }
        }
    }

    private void applySlow(Player target) {
        debuffManager.tryApply(target, DebuffType.SLOW, SLOW_DURATION_TICKS);
    }

    private void applySpike(Player target) {
        Vector velocity = target.getVelocity();
        if (velocity.getY() < 0) {
            target.setVelocity(velocity.setY(velocity.getY() * 2));
        }
    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage and slowing enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + ""),
                new AbilityStat("Range", 8 + ""),
                new AbilityStat("Slow duration", (int) SLOW_DURATION_TICKS / 1000 + "")
        );
    }
}