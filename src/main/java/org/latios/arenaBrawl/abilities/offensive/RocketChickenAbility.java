package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.general.TrackedLivingProjectileTask;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class RocketChickenAbility implements Ability {

    private static final double DAMAGE = 80.0;
    private static final double ENERGY_COST = 30.0;
    private static final double LAUNCH_SPEED = 2.2;
    private static final double DIRECT_HIT_RADIUS = 1.2;
    private static final double AOE_RADIUS = 2.5;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public RocketChickenAbility(EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Rocket Chicken"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Launches a chicken like a rocket.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf(DAMAGE)),
                new AbilityStat("AoE Radius", String.valueOf(AOE_RADIUS)),
                new AbilityStat("Energy Cost", String.valueOf(ENERGY_COST)
                ) );

    }

    @Override
    public boolean activate(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Chicken chicken = player.getWorld().spawn(eye, Chicken.class, c -> {
            c.setAI(true);
            c.setInvulnerable(true);
            c.setSilent(false);
            c.setGravity(false);
            c.setCollidable(false);
        });

        EntityCleanupUtils.markAsArenaEntity(chicken);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_HURT, 1f, 1.4f);

        new TrackedLivingProjectileTask(
                chicken, player, direction, LAUNCH_SPEED, DAMAGE, DIRECT_HIT_RADIUS, AOE_RADIUS,
                getName(), teamManager, combatService,
                Particle.POOF, 15,
                Sound.ENTITY_CHICKEN_HURT, 1.0f, 0.6f
        ).runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }
}