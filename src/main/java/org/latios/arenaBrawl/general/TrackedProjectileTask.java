package org.latios.arenaBrawl.general;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.offensive.ProjectileImpactEffect;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.concurrent.ThreadLocalRandom;

public class TrackedProjectileTask extends BukkitRunnable {

    private static final double HITBOX_EXPAND = 0.3;
    private static final int MAX_LIFETIME_TICKS = 20 * 15;
    private static final double TRAIL_STEP = 0.4;
    private static final int TRAIL_SKIP_TICKS = 2;

    private final Projectile projectile;
    private final Player shooter;
    private final double damage;
    private final double aoeRadius;
    private final String abilityName;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final DebuffType debuffType;
    private final long debuffDuration;
    private final double debuffChance;
    private final ProjectileImpactEffect impactEffect;

    private Particle trailParticle = null;
    private Location lastLocation;
    private int ticksLived = 0;
    private boolean exploded = false;

    public TrackedProjectileTask(Projectile projectile, Player shooter, double damage, double aoeRadius,
                                 String abilityName, TeamManager teamManager, CombatService combatService,
                                 DebuffManager debuffManager, DebuffType debuffType, long debuffDuration,
                                 double debuffChance, ProjectileImpactEffect impactEffect) {
        this.projectile = projectile;
        this.shooter = shooter;
        this.damage = damage;
        this.aoeRadius = aoeRadius;
        this.abilityName = abilityName;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.debuffType = debuffType;
        this.debuffDuration = debuffDuration;
        this.debuffChance = debuffChance;
        this.impactEffect = impactEffect != null ? impactEffect : ProjectileImpactEffect.DEFAULT;
        this.lastLocation = projectile.getLocation().clone();

    }

    public TrackedProjectileTask(Projectile projectile, Player shooter, double damage, double aoeRadius,
                                 String abilityName, TeamManager teamManager, CombatService combatService,
                                 ProjectileImpactEffect impactEffect) {
        this(projectile, shooter, damage, aoeRadius, abilityName, teamManager, combatService, null, null, 0, 0, impactEffect);
    }


    public TrackedProjectileTask withTrail(Particle particle) {
        this.trailParticle = particle;
        return this;
    }

    @Override
    public void run() {
        if (exploded) {
            cancel();
            return;
        }

        if (!projectile.isValid()) {
            explodeAt(lastLocation, null);
            return;
        }

        if (++ticksLived > MAX_LIFETIME_TICKS) {
            cancel();
            projectile.remove();
            return;
        }

        World world = projectile.getWorld();
        Location now = projectile.getLocation();
        Vector velocity = projectile.getVelocity();
        double speed = velocity.length();

        if (speed > 1.0E-6) {
            Vector direction = velocity.clone().normalize();

            RayTraceResult hit = world.rayTrace(
                    now, direction, speed,
                    FluidCollisionMode.NEVER, true, HITBOX_EXPAND,
                    this::isValidTarget);

            if (hit != null) {
                Location hitLoc = hit.getHitPosition().toLocation(world);
                Entity hitEntity = hit.getHitEntity();
                explodeAt(hitLoc, hitEntity instanceof Player p ? p : null);
                return;
            }

            if (trailParticle != null && ticksLived > TRAIL_SKIP_TICKS) {
                spawnTrail(world, now.toVector(), velocity, speed);
            }
        }

        lastLocation = now.clone();
    }

    private boolean isValidTarget(Entity entity) {
        return entity instanceof Player candidate
                && !candidate.equals(shooter)
                && !candidate.isDead()
                && candidate.getGameMode() != GameMode.SPECTATOR
                && teamManager.isEnemy(shooter, candidate);
    }

    private void spawnTrail(World world, Vector from, Vector velocity, double speed) {
        int steps = Math.max(1, (int) Math.ceil(speed / TRAIL_STEP));
        for (int i = 0; i < steps; i++) {
            Vector p = from.clone().add(velocity.clone().multiply((double) i / steps));
            world.spawnParticle(trailParticle, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
        }
    }

    private void explodeAt(Location center, Player directHitVictim) {
        if (exploded) return;
        exploded = true;
        cancel();

        if (directHitVictim != null) {
            if (debuffManager != null && debuffChance > 0
                    && ThreadLocalRandom.current().nextDouble() < debuffChance) {
                debuffManager.tryApply(directHitVictim, debuffType, debuffDuration);
            }
            combatService.applyAbilityDamage(shooter, directHitVictim, damage, abilityName, center);
        }

        for (Player target : center.getWorld().getPlayers()) {
            if (target.equals(directHitVictim)) continue;
            if (target.isDead() || target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(shooter, target)) continue;
            if (distanceToBox(center, target.getBoundingBox()) > aoeRadius) continue;

            combatService.applyAbilityDamage(shooter, target, damage, abilityName);
        }

        playImpactEffects(center);
        if (projectile.isValid()) projectile.remove();
    }

    private double distanceToBox(Location center, BoundingBox box) {
        double x = Math.max(box.getMinX(), Math.min(center.getX(), box.getMaxX()));
        double y = Math.max(box.getMinY(), Math.min(center.getY(), box.getMaxY()));
        double z = Math.max(box.getMinZ(), Math.min(center.getZ(), box.getMaxZ()));
        return center.toVector().distance(new Vector(x, y, z));
    }

    private void playImpactEffects(Location at) {
        at.getWorld().spawnParticle(
                impactEffect.particle(), at, impactEffect.particleCount(),
                0.2, 0.2, 0.2, 0.05);
        at.getWorld().playSound(at, impactEffect.sound(), impactEffect.volume(), impactEffect.pitch());
    }
}