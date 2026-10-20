package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Short-range cone slash that damages every enemy in front of the caster and
 * applies a strong anti-heal. Designed as a melee-range offensive option in 2v2.
 */
public class Hemorrhage implements Ability {

    private final double damage;
    private final double energyCost;
    private final double range;
    private final double coneRadians;
    private final double antiHealPercent;
    private final long antiHealDurationMs;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public Hemorrhage(EnergyManager energyManager, TeamManager teamManager,
                      CombatService combatService, DebuffManager debuffManager,
                      AbilityConfig config) {
        this.damage = config.getDouble("damage", 90.0);
        this.energyCost = config.getDouble("energy-cost", 40.0);
        this.range = config.getDouble("range", 4.5);
        this.coneRadians = Math.toRadians(config.getDouble("cone-degrees", 80.0)) / 2.0;
        this.antiHealPercent = config.getDouble("antiheal-percent", 40.0);
        this.antiHealDurationMs = config.getLong("antiheal-duration-ms", 4000L);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Hemorrhage";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection().normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SPEAR_ATTACK, 1.2f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.7f, 1.4f);
        drawArc(eye, look);

        for (Player target : config.getMatchManager().getMatchPlayers(player)) {
            if (target.equals(player) || target.isDead()
                    || target.getGameMode() == GameMode.SPECTATOR
                    || !teamManager.isEnemy(player, target)) {
                continue;
            }

            Vector toTarget = target.getLocation()
                    .add(0, target.getHeight() * 0.5, 0)
                    .toVector()
                    .subtract(eye.toVector());

            if (toTarget.lengthSquared() > range * range) continue;
            if (look.angle(toTarget) > coneRadians) continue;

            combatService.applyAbilityDamage(
                    player, target, damage, getName(),
                    target.getLocation().add(0, target.getHeight() * 0.5, 0)
            );
            debuffManager.tryApply(
                    player, target, DebuffType.ANTIHEAL,
                    antiHealDurationMs, antiHealPercent
            );

            target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 0.8f);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_SLIME_BLOCK_BREAK, 0.8f, 0.7f);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9f, 0.8f);
            target.getWorld().spawnParticle(
                    Particle.DUST, target.getLocation().add(0, 1.0, 0), 14,
                    0.35, 0.5, 0.35,
                    new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.2f)
            );
            target.getWorld().spawnParticle(
                    Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.0, 0), 8,
                    0.3, 0.4, 0.3, 0.08
            );
        }

        return true;
    }

    private void drawArc(Location eye, Vector look) {
        for (double angle = -coneRadians; angle <= coneRadians; angle += Math.toRadians(9)) {
            Vector direction = look.clone().rotateAroundY(angle);
            Location point = eye.clone().add(direction.multiply(range * 0.85));
            point.getWorld().spawnParticle(
                    Particle.DUST, point, 1, 0.0, 0.0, 0.0,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.4f)
            );
        }
    }

    @Override
    public String getDescription() {
        return "Slashes a short cone in front of you, damaging all enemies and applying " +
                (int) antiHealPercent + "% anti-heal for " + (antiHealDurationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Range", String.format("%.1f blocks", range)),
                new AbilityStat("Anti-Heal", (int) antiHealPercent + "% for " + (antiHealDurationMs / 1000L) + "s")
        );
    }
}
