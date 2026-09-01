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
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.general.TrackedLivingProjectileTask;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class RocketChicken implements Ability {

    private final double damage;
    private final double energyCost;
    private final double launchSpeed;
    private final double directHitRadius;
    private final double aoeRadius;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public RocketChicken(EnergyManager energyManager, TeamManager teamManager,
                         CombatService combatService, AbilityConfig config) {
        this.damage = config.getDouble("damage", 80.0);
        this.energyCost = config.getDouble("energy-cost", 30.0);
        this.launchSpeed = config.getDouble("launch-speed", 2.2);
        this.directHitRadius = config.getDouble("direct-hit-radius", 1.2);
        this.aoeRadius = config.getDouble("aoe-radius", 2.5);

        this.cost = new EnergyCost(energyManager, energyCost);
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
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("AoE Radius", String.valueOf(aoeRadius)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
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
                chicken, player, direction, launchSpeed, damage, directHitRadius, aoeRadius,
                getName(), teamManager, combatService,
                Particle.POOF, 15,
                Sound.ENTITY_CHICKEN_HURT, 1.0f, 0.6f
        ).runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }
}