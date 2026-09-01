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

public class AncientBreath extends Breath {

    private final double damage;
    private final double energyCost;
    private final long immoDurationTicks;
    private final double immoChance;
    private final double range;

    public AncientBreath(EnergyManager energyManager, TeamManager teamManager,
                         CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        super(energyManager, config.getDouble("energy-cost", 75.0), teamManager, combatService, debuffManager);
        this.damage = config.getDouble("damage", 185.0);
        this.energyCost = config.getDouble("energy-cost", 75.0);
        this.immoDurationTicks = config.getLong("immo-duration-ticks", 2000L);
        this.immoChance = config.getDouble("immo-chance", 0.50);
        this.range = config.getDouble("range", 8.0);
    }

    @Override
    public String getName() { return "Ancient Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_HORSE_DEATH;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, point, 1, 0, 0, 0, 0);
    }

    @Override
    protected void applyHitEffects(Player target) {
        if (Math.random() < immoChance) {
            debuffManager.tryApply(target, DebuffType.IMMOBILIZE, immoDurationTicks);
        }
    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage and having a chance to immobilize enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", String.valueOf((int) range)),
                new AbilityStat("Immobilization chance", (int) (immoChance * 100) + "%"),
                new AbilityStat("Immobilization duration", (int) immoDurationTicks / 1000 + "s")
        );
    }
}