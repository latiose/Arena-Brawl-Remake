package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;

import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;

import org.latios.arenaBrawl.abilities.cost.EnergyCost;

import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.TrackedProjectileTask;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class FireballAbility implements Ability {

    private final AbilityCost cost;
    private static final double DAMAGE = 105.0;
    private static final double ENERGY_COST = 40.0;
    private static final double AOE_RADIUS = 3.0;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public FireballAbility(EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Fireball"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Fireball fireball = AbilityProjectileFactory.launchAoe(
                player, Fireball.class, DAMAGE, getName(), AOE_RADIUS
        );
        fireball.setYield(0f);
        fireball.setIsIncendiary(false);
        new TrackedProjectileTask(fireball, player, DAMAGE, AOE_RADIUS, getName(), teamManager, combatService)
                .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        return true;
    }

    @Override
    public String getDescription() {
        return "Launches a fireball, dealing damage to enemies in an area";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("AoE Radius", AOE_RADIUS + " blocks"),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + ""),
                new AbilityStat("Range", "Unlimited")
        );
    }
}