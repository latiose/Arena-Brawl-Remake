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

public class FeralScream extends Breath {

    private final double damage;
    private final double energyCost;
    private final long silenceDurationMs;
    private int trailStep;

    public FeralScream(EnergyManager energyManager, TeamManager teamManager,
                       CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        super(energyManager, config.getDouble("energy-cost", 100.0), teamManager, combatService,
                debuffManager, config.getMatchManager());
        this.damage = config.getDouble("damage", 230.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.silenceDurationMs = config.getLong("silence-duration-ms", 2000L);
    }

    @Override
    public String getName() {
        return "Feral Scream";
    }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_RAVAGER_ROAR;
    }

    @Override
    protected float getCastSoundPitch() {
        return 0.7f;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.SCULK_SOUL, point, 1, 0.08, 0.08, 0.08, 0.01);
        if (trailStep++ % 10 == 0) {
            point.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void applyHitEffects(Player caster, Player target) {
        debuffManager.tryApply(caster, target, DebuffType.SILENCE, silenceDurationMs);
    }

    @Override
    public String getDescription() {
        return "Unleashes a feral scream in a cone in front of the caster, damaging and silencing every enemy caught inside.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Silence", (silenceDurationMs / 1000.0) + "s"),
                new AbilityStat("Range", "8 blocks"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
