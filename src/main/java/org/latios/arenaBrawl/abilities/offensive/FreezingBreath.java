package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class FreezingBreath extends Breath {

    private final double damage;
    private final double energyCost;
    private final long slowDurationTicks;

    public FreezingBreath(EnergyManager energyManager, TeamManager teamManager,
                          CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        super(energyManager, config.getDouble("energy-cost", 80.0), teamManager, combatService, debuffManager);
        this.damage = config.getDouble("damage", 220.0);
        this.energyCost = config.getDouble("energy-cost", 80.0);
        this.slowDurationTicks = config.getLong("slow-duration-ticks", 2000L);
    }

    @Override
    public String getName() { return "Freezing Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_ENDER_DRAGON_GROWL;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 1, 0, 0, 0, 0);
        point.getWorld().spawnParticle(Particle.DRIPPING_WATER, point, 1, 0, 0, 0, 0);
    }

    @Override
    protected void applyHitEffects(Player target) {
        debuffManager.tryApply(target, DebuffType.SLOW, slowDurationTicks);
    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage and slowing enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", "8"),
                new AbilityStat("Slow duration", (int) (slowDurationTicks / 1000L) + "s")
        );
    }
}