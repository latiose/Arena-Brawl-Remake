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

public class SmokeBreath extends Breath {

    private final double damage;
    private final double energyCost;
    private final long blindnessDuration;
    private final AbilityConfig config;

    public SmokeBreath(EnergyManager energyManager, TeamManager teamManager,
                       CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        super(energyManager, config.getDouble("energy-cost", 100.0), teamManager, combatService, debuffManager, config.getMatchManager());
        this.damage = config.getDouble("damage", 250.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.blindnessDuration = config.getLong("blindness-duration-ticks", 2000L);
        this.config = config;
    }

    @Override
    public String getName() { return "Smoke Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_ENDER_DRAGON_GROWL;
    }

    @Override
    protected float getCastSoundPitch() {
        return 0.6f;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, point, 1, 0, 0, 0, 0);
        point.getWorld().spawnParticle(Particle.SMOKE, point, 1, 0, 0, 0, 0);
    }

    @Override
    protected void applyHitEffects(Player caster, Player target) {
        debuffManager.tryApply(target, DebuffType.BLINDNESS, blindnessDuration);
    }

    @Override
    public String getDescription() {
        return "Breathes a cloud of smoke in front of the user, dealing damage and blinding enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", "8"),
                new AbilityStat("Blindness duration", (int) (blindnessDuration / 1000L) + "s")
        );
    }
}
