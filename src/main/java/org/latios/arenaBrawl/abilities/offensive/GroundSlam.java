package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;

import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class GroundSlam implements Ability {

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private static final double ENERGY_COST = 100.0;
    private static final double DAMAGE = 250;

    private final CombatService combatService;

    public GroundSlam(TeamManager teamManager, EnergyManager energyManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Ground slam"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        boolean hitSomeone = false;
        for (Entity nearby : player.getNearbyEntities(4, 3, 4)) {
            if (nearby instanceof Player target && teamManager.isEnemy(player, target) && target.getGameMode() != GameMode.SPECTATOR) {
                combatService.applyAbilityDamage(player, target, DAMAGE, getName());
                target.setVelocity(target.getVelocity().setY(0.5));
                hitSomeone = true;
            }
        }
        if (!hitSomeone) {
            player.sendMessage("§cNo player within range!");
        }
        return hitSomeone;
    }

    @Override
    public String getDescription() {
        return "Shakes the ground around the user, dealing damage and knocking up nearby enemies.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + ""),
                new AbilityStat("Range", 4 + "")
        );
    }
}