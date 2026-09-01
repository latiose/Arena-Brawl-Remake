package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.TrackedProjectileTask;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class SnowballAbility implements Ability {

    private final double damage;
    private final double energyCost;
    private final long slowDurationTicks;
    private final double slowChance;

    private final AbilityCost cost;
    private final DebuffManager debuffManager;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public SnowballAbility(EnergyManager energyManager, TeamManager teamManager,
                           CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        this.damage = config.getDouble("damage", 60.0);
        this.energyCost = config.getDouble("energy-cost", 20.0);
        this.slowDurationTicks = config.getLong("slow-duration-ticks", 2000L);
        this.slowChance = config.getDouble("slow-chance", 0.20);

        this.cost = new EnergyCost(energyManager, energyCost);
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
                player, Snowball.class, damage, getName(), 0
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);
        new TrackedProjectileTask(snowball, player, damage, 1, getName(), teamManager, combatService, debuffManager, DebuffType.SLOW, slowDurationTicks, slowChance, ProjectileImpactEffect.SNOWBALL)
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
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Slow chance", (int) (slowChance * 100) + "%"),
                new AbilityStat("Slow duration", (int) (slowDurationTicks / 1000L) + "s")
        );
    }
}