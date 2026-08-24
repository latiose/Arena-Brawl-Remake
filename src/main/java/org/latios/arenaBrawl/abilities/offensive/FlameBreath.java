package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlameBreath implements Ability {

    private static final double DAMAGE = 150.0;
    private static final double TICK_DAMAGE = 25.0;
    private static final double ENERGY_COST = 60.0;
    private static final double HIT_RADIUS = 2.5;

    private static final double MAX_DISTANCE = 8.0;
    private static final double STEP_SIZE = 0.2;
    private static final double RADIUS_GROWTH = 0.22;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Plugin plugin;

    public FlameBreath(Plugin plugin, EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.plugin = plugin;
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Flame Breath"; }

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

        List<List<Location>> slices = new ArrayList<>();
        List<Location> allConePoints = new ArrayList<>();
        Set<Player> initialHitPlayers = new HashSet<>();

        double currentAngle = 0.0;

        for (double distance = STEP_SIZE; distance <= MAX_DISTANCE; distance += STEP_SIZE) {
            double radius = distance * RADIUS_GROWTH;
            double deltaTheta = 0.35 + (0.05 / (distance + 0.1));
            currentAngle += deltaTheta;

            Location center = origin.clone().add(axis.clone().multiply(distance));

            Location strand1 = helixPoint(center, right, up, radius, currentAngle);
            Location strand2 = helixPoint(center, right, up, radius, currentAngle + Math.PI);

            List<Location> slice = List.of(strand1, strand2);
            slices.add(slice);
            allConePoints.addAll(slice);

            checkHit(player, strand1, initialHitPlayers);
            checkHit(player, strand2, initialHitPlayers);
        }

        for (Player target : initialHitPlayers) {
            combatService.applyAbilityDamage(player, target, DAMAGE, getName());
            applySpike(target);
        }

        int totalSlices = slices.size();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;

                if (tick <= 20) {
                    int maxSliceToSpawn = (int) Math.ceil((double) tick / 20.0 * totalSlices);

                    for (int i = 0; i < Math.min(maxSliceToSpawn, totalSlices); i++) {
                        for (Location pt : slices.get(i)) {
                            spawnTrailParticles(pt);
                        }
                    }
                } else {
                    for (Location pt : allConePoints) {
                        pt.getWorld().spawnParticle(Particle.FLAME, pt, 1, 0.05, 0.05, 0.05, 0);
                    }
                }

                if (tick == 20 || tick == 40) {
                    Set<Player> tickHitPlayers = new HashSet<>();
                    for (Location pt : allConePoints) {
                        checkHit(player, pt, tickHitPlayers);
                    }

                    for (Player target : tickHitPlayers) {
                        combatService.applyAbilityDamage(player, target, TICK_DAMAGE, getName());
                    }
                }

                if (tick >= 40) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    private Location helixPoint(Location center, Vector right, Vector up, double radius, double angle) {
        Vector offset = right.clone().multiply(Math.cos(angle) * radius)
                .add(up.clone().multiply(Math.sin(angle) * radius));
        return center.clone().add(offset);
    }

    private void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.FLAME, point, 1, 0, 0, 0, 0);
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

    private void applySpike(Player target) {
        Vector velocity = target.getVelocity();
        if (velocity.getY() < 0) {
            target.setVelocity(velocity.setY(velocity.getY() * 2));
        }
    }
}