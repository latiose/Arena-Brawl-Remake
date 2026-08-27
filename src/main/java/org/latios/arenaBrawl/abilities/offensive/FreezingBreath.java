package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class FreezingBreath extends Breath {

    private static final double DAMAGE = 220.0;
    private static final double ENERGY_COST = 80.0;
    private static final long SLOW_DURATION_TICKS = 2_000;

    public FreezingBreath(EnergyManager energyManager, TeamManager teamManager,
                          CombatService combatService, DebuffManager debuffManager) {
        super(energyManager, ENERGY_COST, teamManager, combatService, debuffManager);
    }

    @Override
    public String getName() { return "Freezing Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_ENDER_DRAGON_GROWL;
    }

    @Override
    protected double getDamage() {
        return DAMAGE;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 3, 0, 0, 0, 0);
        point.getWorld().spawnParticle(Particle.DRIPPING_WATER, point, 1, 0, 0, 0, 0);
    }

    @Override
    protected void applyHitEffects(Player target) {
        debuffManager.tryApply(target, DebuffType.SLOW, SLOW_DURATION_TICKS);
    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage and slowing enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + ""),
                new AbilityStat("Range", "8"),
                new AbilityStat("Slow duration", (int) SLOW_DURATION_TICKS / 1000 + "")
        );
    }
}