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

public class EvilBreath extends Breath {

    private static final double DAMAGE = 210.0;
    private static final double ENERGY_COST = 100.0;
    private static final long STUN_DURATION_TICKS = 1_000;

    public EvilBreath(EnergyManager energyManager, TeamManager teamManager,
                          CombatService combatService, DebuffManager debuffManager) {
        super(energyManager, ENERGY_COST, teamManager, combatService, debuffManager);
    }

    @Override
    public String getName() { return "Evil Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_WARDEN_ROAR;
    }

    @Override
    protected double getDamage() {
        return DAMAGE;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.SOUL, point, 2, 0.1, 0.1, 0.1, 0.01);
        point.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 2, 0.1, 0.1, 0.1, 0.01);
    }

    @Override
    protected void applyHitEffects(Player target) {
        debuffManager.tryApply(target, DebuffType.STUN, STUN_DURATION_TICKS);
    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage and stunning enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + ""),
                new AbilityStat("Range", "8"),
                new AbilityStat("Slow duration", (int) STUN_DURATION_TICKS / 1000 + "")
        );
    }
}