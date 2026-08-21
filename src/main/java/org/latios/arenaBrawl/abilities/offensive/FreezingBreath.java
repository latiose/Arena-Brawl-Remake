// abilities/impl/DragonBreathAbility.java
package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;

import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FreezingBreath implements Ability {

    private static final double DAMAGE = 220.0;
    private static final double ENERGY_COST = 80.0;
    private static final double SLOWED_SPEED = 0.0625;
    private static final long SLOW_DURATION_TICKS = 40;
    private static final double HIT_RADIUS = 2.5;

    // Exactly 8 blocks from the player's position, as specified
    private static final double MAX_DISTANCE = 8.0;
    private static final double STEP_SIZE = 0.2;

    // Double helix shape: radius grows linearly with distance (cone), two strands
    // 180 degrees apart in rotation phase ("upright" and "upside down").
    private static final double RADIUS_GROWTH = 0.22;   // radius per block travelled
    private static final double ANGULAR_SPEED = 2.4;    // radians per block travelled

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Map<UUID, Double> originalSpeeds = new HashMap<>();

    public FreezingBreath(EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Freezing Breath"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        Location origin = player.getLocation().add(0, 1.4, 0); // roughly chest/eye height
        Vector axis = player.getEyeLocation().getDirection().normalize();

        Vector worldUp = new Vector(0, 1, 0);
        Vector right = axis.clone().crossProduct(worldUp);
        if (right.lengthSquared() < 1e-6) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        Vector up = right.clone().crossProduct(axis).normalize();

        Set<Player> hitPlayers = new HashSet<>();

        for (double distance = STEP_SIZE; distance <= MAX_DISTANCE; distance += STEP_SIZE) {
            double radius = distance * RADIUS_GROWTH;
            double angle = distance * ANGULAR_SPEED;

            Location center = origin.clone().add(axis.clone().multiply(distance));

            // Strand 1 ("upright")
            Location strand1 = helixPoint(center, right, up, radius, angle);
            // Strand 2 ("upside down"), phase-shifted 180 degrees
            Location strand2 = helixPoint(center, right, up, radius, angle + Math.PI);

            spawnTrailParticles(strand1);
            spawnTrailParticles(strand2);

            checkHit(player, strand1, hitPlayers);
            checkHit(player, strand2, hitPlayers);
        }

        for (Player target : hitPlayers) {
            combatService.applyAbilityDamage(player, target, DAMAGE, getName());
            applySlow(target);
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
       // point.getWorld().spawnParticle(Particle.SNOWBALL, point, 3, 0, 0, 0, 0);
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
        AttributeInstance speedAttribute = target.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        double originalValue = originalSpeeds.computeIfAbsent(
                target.getUniqueId(), id -> speedAttribute.getBaseValue()
        );
        speedAttribute.setBaseValue(SLOWED_SPEED);

        org.bukkit.Bukkit.getScheduler().runTaskLater(ArenaBrawlPlugin.getInstance(), () -> {
            AttributeInstance attr = target.getAttribute(Attribute.MOVEMENT_SPEED);
            if (attr != null) {
                attr.setBaseValue(originalValue);
            }
            originalSpeeds.remove(target.getUniqueId());
        }, SLOW_DURATION_TICKS);
    }

    private void applySpike(Player target) {
        Vector velocity = target.getVelocity();
        if (velocity.getY() < 0) {
            target.setVelocity(velocity.setY(velocity.getY() * 2));
        }
    }
}