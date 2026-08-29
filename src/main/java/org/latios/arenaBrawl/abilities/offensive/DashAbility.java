// abilities/impl/DashAbility.java
package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;

import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashAbility implements Ability {

    private static final double DISTANCE = 3.0;
    private static final double DAMAGE = 100.0;
    private static final double ENERGY_COST = 40.0;
    private static final double STEP_SIZE = 0.2;
    private static final double HIT_RADIUS = 1.3;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public DashAbility(EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Dash"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Dashes 3 blocks forward, dealing 100 damage to any enemy caught in your path. Stops at walls.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Distance", "3 blocks"),
                new AbilityStat("Damage", "100"),
                new AbilityStat("Energy Cost", "40")
        );
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Location current = player.getLocation();

        double traveled = 0;
        Location lastSafe = current.clone();
        Set<Player> hit = new HashSet<>();

        while (traveled < DISTANCE) {
            Location next = lastSafe.clone().add(direction.clone().multiply(STEP_SIZE));

            if (isBlocked(next)) {
                break;
            }

            for (Entity nearby : next.getWorld().getNearbyEntities(next, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (nearby instanceof Player target && !target.equals(player)
                        && teamManager.isEnemy(player, target) && !hit.contains(target)) {
                    combatService.applyAbilityDamage(player, target, DAMAGE, getName());
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