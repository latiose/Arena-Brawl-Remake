package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * A precise execution shot. It deals normal damage and finishes the target when
 * the hit would leave them below the configured health threshold.
 */
public class Cull implements Ability {

    private final double energyCost;
    private final double damage;
    private final double executeThreshold;
    private final double executeBonus;
    private final double maxRange;
    private final double hitboxRadius;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Cull(EnergyManager energyManager, TeamManager teamManager, CombatService combatService,
                PlayerHealthManager healthManager, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.damage = config.getDouble("damage", 150.0);
        this.executeThreshold = Math.max(0.0, Math.min(1.0, config.getDouble("execute-threshold", 0.30)));
        this.executeBonus = config.getDouble("execute-bonus", 250.0);
        this.maxRange = config.getDouble("max-range", 20.0);
        this.hitboxRadius = Math.max(0.2, config.getDouble("hitbox-radius", 0.8));

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Cull";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.8f, 1.5f);

        Player victim = findTarget(player, eye, direction);

        if (victim == null) {
            drawTrail(eye, eye.clone().add(direction.clone().multiply(maxRange)));
            return true;
        }

        Location impact = victim.getLocation().add(0, 1.0, 0);
        drawTrail(eye, impact);

        double currentHealth = healthManager.getHealth(victim);
        double maxHealth = Math.max(1.0, healthManager.getMaxHealth(victim));
        boolean executes = currentHealth - damage <= maxHealth * executeThreshold;

        double totalDamage = damage + (executes ? executeBonus : 0.0);
        combatService.applyAbilityDamage(player, victim, totalDamage, getName(), impact);

        if (executes) {
            impact.getWorld().spawnParticle(Particle.DUST, impact, 25, 0.4, 0.6, 0.4,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 20, 30), 1.4f));
            impact.getWorld().spawnParticle(Particle.CRIT, impact, 20, 0.3, 0.5, 0.3, 0.2);
            impact.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.8f);
        } else {
            impact.getWorld().spawnParticle(Particle.CRIT, impact, 10, 0.25, 0.4, 0.25, 0.1);
            impact.getWorld().playSound(impact, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        }

        return true;
    }

    private Player findTarget(Player caster, Location eye, Vector direction) {
        RayTraceResult result = caster.getWorld().rayTrace(
                eye, direction, maxRange, FluidCollisionMode.NEVER, true, hitboxRadius,
                entity -> isValidTarget(caster, entity)
        );

        if (result == null) return null;
        if (result.getHitEntity() instanceof Player target) return target;
        return null;
    }

    private boolean isValidTarget(Player caster, Entity entity) {
        if (!(entity instanceof Player target)) return false;
        if (target.equals(caster) || target.isDead()) return false;
        if (target.getGameMode() == GameMode.SPECTATOR) return false;
        if (!config.getMatchManager().isInMatch(target)) return false;
        return teamManager.isEnemy(caster, target);
    }

    private void drawTrail(Location from, Location to) {
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        if (length < 0.1) return;

        Vector step = delta.clone().normalize().multiply(0.5);
        Location point = from.clone();

        for (double travelled = 0; travelled < length; travelled += 0.5) {
            point.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
            point.add(step);
        }
    }

    @Override
    public String getDescription() {
        return "Fires an execution shot. If the hit would leave the target below "
                + (int) (executeThreshold * 100) + "% health, it deals a large finishing blow.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) damage + " HP"),
                new AbilityStat("Execute threshold", (int) (executeThreshold * 100) + "%"),
                new AbilityStat("Execute bonus", (int) executeBonus + " HP"),
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
