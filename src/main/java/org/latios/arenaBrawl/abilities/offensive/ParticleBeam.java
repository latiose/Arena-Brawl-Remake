package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParticleBeam implements Ability {

    private final double damage;
    private final double energyCost;
    private final double maxRange;
    private final double stepSize;
    private final double hitExpansion;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public ParticleBeam(EnergyManager energyManager, TeamManager teamManager,
                        CombatService combatService, AbilityConfig config) {
        this.damage = config.getDouble("damage", 130.0);
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.maxRange = config.getDouble("max-range", 256.0);
        this.stepSize = config.getDouble("step-size", 0.4);
        this.hitExpansion = config.getDouble("hit-expansion", 0.4);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Particle Beam"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Fires an instant beam of particles in a straight line, dealing damage to all enemies it passes through";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Range", "Infinite")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Set<Player> hitEnemies = new HashSet<>();

        Set<Player> potentialEnemies = new HashSet<>();
        for (Entity entity : player.getWorld().getNearbyEntities(eye, maxRange, maxRange, maxRange)) {
            if (entity instanceof Player candidate && !candidate.equals(player) && teamManager.isEnemy(player, candidate)) {
                potentialEnemies.add(candidate);
            }
        }

        for (double distance = stepSize; distance <= maxRange; distance += stepSize) {
            Location point = eye.clone().add(direction.clone().multiply(distance));

            if (point.getBlock().getType().isSolid()) {
                break;
            }

            point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);

            Vector pointVector = point.toVector();
            for (Player candidate : potentialEnemies) {
                BoundingBox box = candidate.getBoundingBox().expand(hitExpansion);
                if (box.contains(pointVector)) {
                    hitEnemies.add(candidate);
                }
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2f, 2f);

        for (Player victim : hitEnemies) {
            combatService.applyAbilityDamage(player, victim, damage, getName());
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 20, 0.2, 0.4, 0.2, 0.1);
        }

        return true;
    }
}