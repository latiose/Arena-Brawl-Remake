package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import org.bukkit.entity.Snowball;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;

import org.latios.arenaBrawl.abilities.cost.EnergyCost;

import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.TrackedProjectileTask;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class SnowballAbility implements Ability {

    private final AbilityCost cost;
    private static final double DAMAGE = 60.0;
    private static final double ENERGY_COST = 20.0;
    private static final long SLOW_DURATION = 2_000;
    private static final double slowChance = 0.2;
    private final DebuffManager debuffManager;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public SnowballAbility(EnergyManager energyManager, TeamManager teamManager, CombatService combatService, DebuffManager debuffManager) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Snowball"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Snowball snowball = AbilityProjectileFactory.launchAoe(
                player, Snowball.class, DAMAGE, getName(), 0
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);
        new TrackedProjectileTask(snowball, player, DAMAGE, 1, getName(), teamManager, combatService,debuffManager, DebuffType.SLOW,SLOW_DURATION,slowChance,ProjectileImpactEffect.SNOWBALL)
                .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        return true;
    }

    @Override
    public String getDescription() {
        return "Launches a snowball, dealing damage to enemies and having a chance to slow them";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + ""),
                new AbilityStat("Slow chance", "20%"),
                new AbilityStat("Slow duration", "2s")
        );
    }
}