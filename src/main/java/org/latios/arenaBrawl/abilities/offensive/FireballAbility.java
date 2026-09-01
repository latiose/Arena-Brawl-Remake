package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;

import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;

import org.latios.arenaBrawl.abilities.config.AbilityConfigManager;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.TrackedProjectileTask;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class FireballAbility implements Ability {

    private final AbilityCost cost;
    private final double damage;
    private final double aoeRadius;
    private final double energyCost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public FireballAbility(EnergyManager energyManager, TeamManager teamManager, CombatService combatService,AbilityConfig config) {
        this.damage = config.getDouble("damage", 105.0);
        this.aoeRadius = config.getDouble("aoe-radius", 3.0);
        this.energyCost = config.getDouble("energy-cost", 40.0);
        this.cost = new EnergyCost(energyManager, energyCost);
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
                player, Fireball.class, damage, getName(), aoeRadius
        );
        fireball.setYield(0f);
        fireball.setIsIncendiary(false);
        new TrackedProjectileTask(fireball, player, damage, aoeRadius, getName(), teamManager, combatService,ProjectileImpactEffect.FIREBALL)
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
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("AoE Radius", aoeRadius + " blocks"),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", "Unlimited")
        );
    }
}