package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class Breath implements Ability {
    protected static final double HIT_RADIUS = 2.5;
    protected static final double HIT_RADIUS_SQUARED = HIT_RADIUS * HIT_RADIUS;

    protected static final int POINT_COUNT = 80;
    protected static final double DEPTH_SEPARATION = 0.065;
    protected static final double AMPLITUDE_SCALE = 2.0 / (POINT_COUNT - 1);
    protected static final double ANGLE_STEP = Math.toRadians(15.0);
    protected static final double EYE_HEIGHT = 1.62;
    protected static final double SNEAK_EYE_OFFSET = -0.08;
    protected static final double VERTICAL_WEIGHT = 1.5;
    private static final double[][] CONE = buildCone();

    private static double[][] buildCone() {
        double[][] points = new double[POINT_COUNT][3];
        for (int i = 0; i < POINT_COUNT; i++) {
            double amplitude = i * AMPLITUDE_SCALE;
            points[i][0] = Math.cos(ANGLE_STEP * i) * amplitude;
            points[i][1] = Math.sin(ANGLE_STEP * i) * amplitude;
            points[i][2] = (i + 1) * DEPTH_SEPARATION;
        }
        return points;
    }

    protected final AbilityCost cost;
    protected final TeamManager teamManager;
    protected final CombatService combatService;
    protected final DebuffManager debuffManager;
    protected final Plugin plugin;
    protected final MatchManager matchManager;

    public Breath(EnergyManager energyManager, double energyCost, TeamManager teamManager,
                  CombatService combatService, DebuffManager debuffManager, MatchManager matchManager) {
        this(null, energyManager, energyCost, teamManager, combatService, debuffManager, matchManager);
    }

    public Breath(Plugin plugin, EnergyManager energyManager, double energyCost, TeamManager teamManager,
                  CombatService combatService, DebuffManager debuffManager, MatchManager matchManager) {
        this.plugin = plugin;
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.matchManager = matchManager;
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        player.getWorld().playSound(player.getLocation(), getCastSound(), 10.0f, 1.0f);

        Location eye = player.getLocation().add(0, EYE_HEIGHT + (player.isSneaking() ? SNEAK_EYE_OFFSET : 0), 0);

        double yawRad = Math.toRadians(eye.getYaw());
        double si = Math.sin(yawRad);
        double co = Math.cos(yawRad);

        List<Player> candidates = new ArrayList<>();
        for (Player candidate : eye.getWorld().getPlayers()) {
            if (candidate.equals(player)) continue;
            if (!isTargetValid(player, candidate)) continue;
            candidates.add(candidate);
        }

        Set<Player> hitPlayers = new HashSet<>();

        for (double[] p : CONE) {
            double dx = co * p[0] - si * p[2];
            double dz = si * p[0] + co * p[2];


            Location upright = eye.clone().add(dx, p[1], dz);
            spawnTrailParticles(upright);
            checkHit(upright, candidates, hitPlayers);


            Location inverted = eye.clone().add(dx, -p[1], dz);
            checkHit(inverted, candidates, hitPlayers);
        }

        for (Player target : hitPlayers) {
            applyHit(player, target);
        }

        return true;
    }


    protected void checkHit(Location point, List<Player> candidates, Set<Player> hitPlayers) {
        Iterator<Player> it = candidates.iterator();
        while (it.hasNext()) {
            Player candidate = it.next();
            Location loc = candidate.getLocation();
            double dx = loc.getX() - point.getX();
            double dy = (loc.getY() - point.getY()) * VERTICAL_WEIGHT;
            double dz = loc.getZ() - point.getZ();
            if (dx * dx + dy * dy + dz * dz < HIT_RADIUS_SQUARED) {
                hitPlayers.add(candidate);
                it.remove();
            }
        }
    }

    protected boolean isTargetValid(Player caster, Player candidate) {
        return teamManager.isEnemy(caster, candidate);
    }

    protected void applyHit(Player caster, Player target) {
        applyHitEffects(target);
        combatService.applyAbilityDamage(caster, target, getDamage(), getName());
        applySpike(target);
    }


    protected void applySpike(Player target) {
        if (target.getGameMode() == GameMode.SPECTATOR) return;

        Vector velocity = target.getVelocity();
        if (velocity.getY() < 0) {
            velocity.setY(velocity.getY() * 2.0);
            target.setVelocity(velocity);
        }
    }

    protected List<Location> conePoints(Player player, boolean inverted) {
        Location eye = player.getLocation().add(0, EYE_HEIGHT + (player.isSneaking() ? SNEAK_EYE_OFFSET : 0), 0);
        double yawRad = Math.toRadians(eye.getYaw());
        double si = Math.sin(yawRad);
        double co = Math.cos(yawRad);

        List<Location> points = new ArrayList<>(POINT_COUNT);
        for (double[] p : CONE) {
            double dx = co * p[0] - si * p[2];
            double dz = si * p[0] + co * p[2];
            points.add(eye.clone().add(dx, inverted ? -p[1] : p[1], dz));
        }
        return points;
    }

    protected List<Player> collectCandidates(Player caster) {
        List<Player> candidates = new ArrayList<>();
        for (Player candidate : caster.getWorld().getPlayers()) {
            if (candidate.equals(caster)) continue;
            if (!isTargetValid(caster, candidate)) continue;
            candidates.add(candidate);
        }
        return candidates;
    }

    protected Set<Player> findHits(Player caster, List<Location> points) {
        List<Player> candidates = collectCandidates(caster);
        Set<Player> hitPlayers = new HashSet<>();
        for (Location point : points) {
            if (candidates.isEmpty()) break;
            checkHit(point, candidates, hitPlayers);
        }
        return hitPlayers;
    }

    protected abstract Sound getCastSound();
    protected abstract double getDamage();
    protected abstract void spawnTrailParticles(Location point);
    protected abstract void applyHitEffects(Player target);
}