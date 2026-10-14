package org.latios.arenaBrawl.general;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.Set;

public class TrackedLivingProjectileTask extends BukkitRunnable {

    private static final int MAX_LIFETIME_TICKS = 80;
    private static final double SURFACE_OFFSET = 0.15;

    private final LivingEntity entity;
    private final Player shooter;
    private final Vector velocity;
    private final Vector direction;
    private final double speed;
    private final double damage;
    private final double hitRadius;
    private final double aoeRadius;

    private final String abilityName;
    private final TeamManager teamManager;
    private final CombatService combatService;

    private final Particle impactParticle;
    private final int particleCount;
    private final Sound impactSound;
    private final float soundVolume;
    private final float soundPitch;
    private final boolean explosion;
    private int ticksElapsed = 0;
    private Vector lastCenter;

    public TrackedLivingProjectileTask(LivingEntity entity, Player shooter, Vector direction, double speed,
                                       double damage, double hitRadius, double aoeRadius,
                                       String abilityName, TeamManager teamManager, CombatService combatService,
                                       Particle impactParticle, int particleCount,
                                       Sound impactSound, float soundVolume, float soundPitch) {
        this(entity, shooter, direction, speed, damage, hitRadius, aoeRadius, abilityName,
                teamManager, combatService, impactParticle, particleCount, impactSound, soundVolume, soundPitch, false);
    }

    public TrackedLivingProjectileTask(LivingEntity entity, Player shooter, Vector direction, double speed,
                                       double damage, double hitRadius, double aoeRadius,
                                       String abilityName, TeamManager teamManager, CombatService combatService,
                                       Particle impactParticle, int particleCount,
                                       Sound impactSound, float soundVolume, float soundPitch, boolean explosion) {
        this.entity = entity;
        this.shooter = shooter;
        this.speed = speed;
        this.direction = direction.clone().normalize();
        this.velocity = this.direction.clone().multiply(speed);
        this.damage = damage;
        this.hitRadius = hitRadius;
        this.aoeRadius = aoeRadius;
        this.abilityName = abilityName;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.impactParticle = impactParticle;
        this.particleCount = particleCount;
        this.impactSound = impactSound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
        this.explosion = explosion;

        entity.setCollidable(false);
        entity.setGravity(false);
    }

    @Override
    public void run() {
        if (entity.isDead() || !entity.isValid() || ticksElapsed >= MAX_LIFETIME_TICKS) {
            detonate(entity.getLocation(), null);
            return;
        }

        World world = entity.getWorld();
        BoundingBox body = entity.getBoundingBox();
        Vector center = body.getCenter();
        if (lastCenter == null) lastCenter = center.clone();

        double halfSize = Math.max(body.getHeight(), Math.max(body.getWidthX(), body.getWidthZ())) / 2.0;
        Vector start = lastCenter.clone();
        double length = start.distance(center) + speed + halfSize;

        RayTraceResult blockHit = world.rayTraceBlocks(
                start.toLocation(world),
                direction,
                length,
                FluidCollisionMode.NEVER,
                true
        );
        double blockDistance = blockHit != null ? start.distance(blockHit.getHitPosition()) : Double.MAX_VALUE;

        Player victim = null;
        Vector victimPoint = null;
        double best = Double.MAX_VALUE;

        for (Player candidate : world.getPlayers()) {
            if (candidate.equals(shooter) || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(shooter, candidate)) continue;

            BoundingBox box = candidate.getBoundingBox().expand(hitRadius + halfSize);
            RayTraceResult result = box.rayTrace(start, direction, length);
            if (result == null) continue;

            double distance = start.distance(result.getHitPosition());
            if (distance < best) {
                best = distance;
                victim = candidate;
                victimPoint = result.getHitPosition().clone();
            }
        }

        if (victim != null && best <= blockDistance) {
            detonate(victimPoint.toLocation(world), victim);
            return;
        }

        if (blockHit != null) {
            Vector normal = blockHit.getHitBlockFace() != null
                    ? blockHit.getHitBlockFace().getDirection()
                    : new Vector();
            Vector point = blockHit.getHitPosition().clone().add(normal.multiply(SURFACE_OFFSET));
            detonate(point.toLocation(world), null);
            return;
        }

        entity.setVelocity(velocity);
        lastCenter = center.clone();
        ticksElapsed++;
    }

    private void detonate(Location impact, Player directHitVictim) {
        cancel();

        Set<Player> alreadyHit = new HashSet<>();
        if (directHitVictim != null) {
            combatService.applyAbilityDamage(shooter, directHitVictim, damage, abilityName, impact);
            alreadyHit.add(directHitVictim);
        }

        applyAoeDamage(impact, alreadyHit);
        spawnImpactEffects(impact);
        entity.remove();
    }

    private void applyAoeDamage(Location impact, Set<Player> alreadyHit) {
        if (aoeRadius <= 0) return;

        Vector point = impact.toVector();
        double radiusSquared = aoeRadius * aoeRadius;

        for (Player target : impact.getWorld().getPlayers()) {
            if (alreadyHit.contains(target) || target.equals(shooter) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(shooter, target)) continue;

            BoundingBox box = target.getBoundingBox();
            double cx = Math.max(box.getMinX(), Math.min(point.getX(), box.getMaxX()));
            double cy = Math.max(box.getMinY(), Math.min(point.getY(), box.getMaxY()));
            double cz = Math.max(box.getMinZ(), Math.min(point.getZ(), box.getMaxZ()));
            double dx = cx - point.getX();
            double dy = cy - point.getY();
            double dz = cz - point.getZ();

            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                combatService.applyAbilityDamage(shooter, target, damage, abilityName, impact);
                alreadyHit.add(target);
            }
        }
    }

    private void spawnImpactEffects(Location impact) {
        World world = impact.getWorld();
        if (explosion) {
            world.createExplosion(impact, 0.0f, false, false);
        }
        if (impactParticle != null && particleCount > 0) {
            double spread = aoeRadius > 0 ? Math.min(aoeRadius * 0.4, 1.0) : 0.0;
            world.spawnParticle(impactParticle, impact, particleCount, spread, spread, spread, 0.0);
        }
        if (impactSound != null) {
            world.playSound(impact, impactSound, soundVolume, soundPitch);
        }
    }
}