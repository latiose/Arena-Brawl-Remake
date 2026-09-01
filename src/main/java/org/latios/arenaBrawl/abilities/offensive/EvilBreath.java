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

public class EvilBreath extends Breath {

    private final double damage;
    private final double energyCost;
    private final long stunDurationTicks;

    public EvilBreath(EnergyManager energyManager, TeamManager teamManager,
                      CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        super(energyManager, config.getDouble("energy-cost", 100.0), teamManager, combatService, debuffManager);
        this.damage = config.getDouble("damage", 210.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.stunDurationTicks = config.getLong("stun-duration-ticks", 1000L);
    }

    @Override
    public String getName() { return "Evil Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_WARDEN_ROAR;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.SOUL, point, 2, 0.1, 0.1, 0.1, 0.01);
        point.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 2, 0.1, 0.1, 0.1, 0.01);
    }

    @Override
    protected void applyHitEffects(Player target) {
        debuffManager.tryApply(target, DebuffType.STUN, stunDurationTicks);
    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage and stunning enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", "8"),
                new AbilityStat("Stun duration", (int) (stunDurationTicks / 1000L) + "s")
        );
    }
}