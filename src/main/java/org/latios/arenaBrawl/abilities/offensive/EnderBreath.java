package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class EnderBreath extends Breath {

    private final double damage;
    private final double energyCost;
    private final long energyDenyMillis;
    private final AbilityConfig config;
    private final EnergyModifierManager energyModifierManager;

    public EnderBreath(EnergyManager energyManager, TeamManager teamManager,
                       CombatService combatService, EnergyModifierManager energyModifierManager,
                       AbilityConfig config) {
        super(energyManager, config.getDouble("energy-cost", 100.0), teamManager, combatService,
                null, config.getMatchManager());
        this.damage = config.getDouble("damage", 230.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.energyDenyMillis = config.getLong("energy-deny-ms", 1000L);
        this.energyModifierManager = energyModifierManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Ender Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_ENDER_DRAGON_GROWL;
    }

    @Override
    protected float getCastSoundPitch() {
        // Higher than the other breaths so Ender Breath stands out.
        return 1.5f;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.WITCH, point, 1, 0, 0, 0, 0);
    }

    @Override
    protected void applyHitEffects(Player caster, Player target) {
        // Stop the target's energy regeneration for 1s and duplicate it onto the
        // caster: his regen is doubled for the same window, effectively stealing
        // 1s of the victim's regeneration.
        energyModifierManager.addModifier(target, "ender_breath", 0.0, energyDenyMillis);
        energyModifierManager.addModifier(caster, "ender_breath_steal", 2.0, energyDenyMillis);

        target.getWorld().spawnParticle(
                Particle.WITCH, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.3);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 10.0f, 0.5f);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.7f);
        caster.getWorld().spawnParticle(
                Particle.WITCH, caster.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.2);
    }

    @Override
    public String getDescription() {
        return "Breathes a cone of ender energy, damaging enemies, stopping their energy regeneration for "
                + (energyDenyMillis / 1000L) + "s and duplicating it onto yourself for the same time.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy stolen", (energyDenyMillis / 1000L) + "s"),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", "8")
        );
    }
}
