package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class GuidedPig2000 implements Ability {

    private final double damage;
    private final double energyCost;
    private final double launchSpeed;
    private final double directHitRadius;
    private final double aoeRadius;
    private final double steerFactor;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public GuidedPig2000(EnergyManager energyManager, TeamManager teamManager,
                         CombatService combatService, AbilityConfig config) {
        this.damage = config.getDouble("damage", 265.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.launchSpeed = config.getDouble("launch-speed", 0.85);
        this.directHitRadius = config.getDouble("direct-hit-radius", 1);
        this.aoeRadius = config.getDouble("aoe-radius", 3.0);
        this.steerFactor = config.getDouble("steer-factor", 0.7);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Guided Pig 2000"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Launches a homing pig that slowly steers toward wherever you aim.";
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

        Pig pig = player.getWorld().spawn(eye, Pig.class, p -> {
            p.setAI(true);
            p.setInvulnerable(true);
            p.setSilent(false);
            p.setGravity(false);
            p.setCollidable(false);
        });

        EntityCleanupUtils.markAsArenaEntity(pig);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PIG_DEATH, 10.0f, 0.1f);

        new TrackedLivingProjectileTask(
                pig, player, direction, launchSpeed, damage, directHitRadius, aoeRadius,
                getName(), teamManager, combatService,
                Particle.SMOKE, 15,
                Sound.ENTITY_PIG_DEATH, 5.0f, 1f,
                true, true, steerFactor
        ).runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }
}
