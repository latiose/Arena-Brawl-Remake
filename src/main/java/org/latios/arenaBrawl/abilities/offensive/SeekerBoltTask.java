package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Drives a single Seeker Bolt: it homes towards the nearest enemy while it flies,
 * detonating on the first target (or block) it touches.
 */
public class SeekerBoltTask extends BukkitRunnable {

    private final Projectile projectile;
    private final Player shooter;
    private final double damage;
    private final String abilityName;
    private final double speed;
    private final double turnRate;
    private final double trackRadius;
    private final double hitRadius;
    private final double aoeRadius;
    private final int maxLifetimeTicks;
    private final TeamManager teamManager;
    private final CombatService combatService;

    private Location previous;
    private int ticksLived = 0;
    private boolean finished = false;

    public SeekerBoltTask(Projectile projectile, Player shooter, double damage, String abilityName,
                          double speed, double turnRate, double trackRadius, double hitRadius,
                          double aoeRadius, int maxLifetimeTicks,
                          TeamManager teamManager, CombatService combatService) {
        this.projectile = projectile;
        this.shooter = shooter;
        this.damage = damage;
        this.abilityName = abilityName;
        this.speed = speed;
        this.turnRate = turnRate;
        this.trackRadius = trackRadius;
        this.hitRadius = hitRadius;
        this.aoeRadius = aoeRadius;
        this.maxLifetimeTicks = maxLifetimeTicks;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.previous = projectile.getLocation().clone();
    }

    @Override
    public void run() {
        if (finished) {
            cancel();
            return;
        }

        if (!shooter.isOnline() || shooter.isDead()) {
            finish(projectile.getLocation());
            return;
        }

        if (!projectile.isValid() || projectile.isDead()) {
            finish(previous);
            return;
        }

        if (++ticksLived > maxLifetimeTicks) {
            finish(projectile.getLocation());
            return;
        }

        Location now = projectile.getLocation();

        Vector movement = now.toVector().subtract(previous.toVector());
        double distance = movement.length();
        if (distance > 1.0E-6) {
            RayTraceResult hit = now.getWorld().rayTrace(
                    previous, movement.clone().normalize(), distance,
                    FluidCollisionMode.NEVER, true, hitRadius, this::isEnemy);
            if (hit != null && hit.getHitEntity() instanceof Player victim) {
                Location impact = hit.getHitPosition().toLocation(now.getWorld());
                hitDirect(victim, impact);
                finish(impact);
                return;
            }
        }

        Player target = findNearestEnemy(now);
        Vector velocity = projectile.getVelocity();
        if (target != null) {
            Vector toTarget = target.getLocation().add(0, target.getHeight() * 0.5, 0)
                    .toVector().subtract(now.toVector());
            if (toTarget.lengthSquared() > 1.0E-6) {
                Vector desired = toTarget.normalize().multiply(speed);
                velocity = velocity.multiply(1.0 - turnRate).add(desired.multiply(turnRate));
            }
        }
        if (velocity.lengthSquared() > 1.0E-6) {
            velocity = velocity.normalize().multiply(speed);
        }
        projectile.setVelocity(velocity);
        projectile.setGravity(false);

        now.getWorld().spawnParticle(Particle.END_ROD, now, 3, 0.04, 0.04, 0.04, 0);
        previous = now.clone();
    }

    private void hitDirect(Player victim, Location impact) {
        Set<Player> hit = new HashSet<>();
        combatService.applyAbilityDamage(shooter, victim, damage, abilityName, impact);
        hit.add(victim);

        for (Entity nearby : impact.getWorld().getNearbyEntities(impact, aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && !hit.contains(target) && isEnemy(target)) {
                combatService.applyAbilityDamage(shooter, target, damage, abilityName, impact);
            }
        }
    }

    private void finish(Location impact) {
        if (finished) return;
        finished = true;
        cancel();

        impact.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, impact, 12, 0.15, 0.15, 0.15, 0.02);
        impact.getWorld().spawnParticle(Particle.CRIT, impact, 8, 0.1, 0.1, 0.1, 0.05);
        impact.getWorld().playSound(impact, Sound.ENTITY_ENDER_EYE_DEATH, 0.7f, 1.4f);

        if (projectile.isValid()) projectile.remove();
    }

    private boolean isEnemy(Entity entity) {
        return entity instanceof Player candidate
                && !candidate.equals(shooter)
                && !candidate.isDead()
                && teamManager.isEnemy(shooter, candidate);
    }

    private Player findNearestEnemy(Location from) {
        Player nearest = null;
        double closest = trackRadius * trackRadius;
        for (Player candidate : from.getWorld().getPlayers()) {
            if (!isEnemy(candidate)) continue;
            double distance = candidate.getLocation().distanceSquared(from);
            if (distance <= closest) {
                closest = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }
}
