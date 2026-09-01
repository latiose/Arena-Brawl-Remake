package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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

public class DashAbility implements Ability {

    private static final double STEP_SIZE = 0.2;

    private final AbilityCost cost;
    private final double distance;
    private final double damage;
    private final double energyCost;
    private final double hitRadius;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public DashAbility(EnergyManager energyManager, TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        this.distance = config.getDouble("distance", 4.0);
        this.damage = config.getDouble("damage", 105.0);
        this.energyCost = config.getDouble("energy-cost", 40.0);
        this.hitRadius = config.getDouble("hit-radius", 1.3);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Dash"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Dashes " + (int) distance + " blocks forward, dealing " + (int) damage + " damage to any enemy caught in your path. Stops at walls.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Distance", String.valueOf(distance)),
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Location current = player.getLocation();

        double traveled = 0;
        Location lastSafe = current.clone();
        Set<Player> hit = new HashSet<>();

        while (traveled < distance) {
            Location next = lastSafe.clone().add(direction.clone().multiply(STEP_SIZE));

            if (isBlocked(next)) {
                break;
            }

            for (Entity nearby : next.getWorld().getNearbyEntities(next, hitRadius, hitRadius, hitRadius)) {
                if (nearby instanceof Player target && !target.equals(player)
                        && teamManager.isEnemy(player, target) && !hit.contains(target)) {
                    combatService.applyAbilityDamage(player, target, damage, getName());
                    hit.add(target);
                }
            }

            lastSafe = next;
            traveled += STEP_SIZE;
        }

        player.teleport(lastSafe);
        player.getWorld().spawnParticle(Particle.CLOUD, current, 15, 0.2, 0.2, 0.2, 0.02);

        return true;
    }

    private boolean isBlocked(Location destination) {
        var block = destination.getBlock();
        var above = destination.clone().add(0, 1, 0).getBlock();
        return block.getType().isSolid() || above.getType().isSolid();
    }
}