package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class HeavensBreath extends Breath {

    private final double damage;
    private final double energyCost;
    private final double healAmount;
    private final PlayerHealthManager healthManager;

    public HeavensBreath(EnergyManager energyManager, TeamManager teamManager,
                         CombatService combatService, PlayerHealthManager healthManager, AbilityConfig config) {
        super(energyManager, config.getDouble("energy-cost", 100.0), teamManager, combatService, null);
        this.damage = config.getDouble("damage", 190.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.healAmount = config.getDouble("heal-amount", 50.0);
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Heavens Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.BLOCK_BEACON_ACTIVATE;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected boolean isTargetValid(Player caster, Player candidate) {
        return true;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.END_ROD, point, 2, 0.05, 0.05, 0.05, 0.01);
        point.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, point, 1, 0.05, 0.05, 0.05, 0.0);
    }

    @Override
    public boolean activate(Player player) {
        boolean activated = super.activate(player);

        if (activated) {
            healthManager.heal(player, healAmount, getName());
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.0);
        }

        return activated;
    }

    @Override
    protected void applyHit(Player caster, Player target) {
        if (teamManager.isEnemy(caster, target)) {
            applyHitEffects(target);
            combatService.applyAbilityDamage(caster, target, getDamage(), getName());
            applySpike(target);
        } else {
            healthManager.healAlly(caster, target, healAmount, getName());
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.0);
        }
    }

    @Override
    protected void applyHitEffects(Player target) {

    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage to enemies and healing allies caught inside.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Range", "8"),
                new AbilityStat("Heal Amount", String.valueOf((int) healAmount))
        );
    }
}