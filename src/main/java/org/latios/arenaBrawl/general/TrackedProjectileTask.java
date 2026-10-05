package org.latios.arenaBrawl.general;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.World;
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

    private static final double HITBOX_EXPAND = 0.3;   // margen extra alrededor del jugador
    private static final int MAX_LIFETIME_TICKS = 20 * 15;

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

    @Override
    public void run() {
        if (exploded) {
            cancel();
            return;
        }

        if (projectile.isDead() || !projectile.isValid()) {
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
        Vector from = lastLocation.toVector();
        Vector to = now.toVector();
        Vector move = to.clone().subtract(from);
        double distance = move.length();

        Vector direction = distance > 1.0E-6 ? move.clone().normalize() : projectile.getVelocity().clone();
        if (direction.lengthSquared() < 1.0E-6) {
            direction = new Vector(0, -1, 0);
        } else {
            direction.normalize();
        }

        Player hitPlayer = null;
        double playerDist = Double.MAX_VALUE;
        Vector playerHitPos = null;

        for (Player candidate : world.getPlayers()) {
            if (candidate.equals(shooter)) continue;
            if (candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(shooter, candidate)) continue;

            BoundingBox box = candidate.getBoundingBox().expand(HITBOX_EXPAND);

            if (box.contains(to) || box.contains(from)) {
                double d = box.contains(from) ? 0 : distance;
                if (d < playerDist) {
                    playerDist = d;
                    hitPlayer = candidate;
                    playerHitPos = box.contains(from) ? from.clone() : to.clone();
                }
                continue;
            }

            if (distance > 1.0E-6) {
                RayTraceResult r = box.rayTrace(from, direction, distance);
                if (r != null) {
                    double d = r.getHitPosition().distance(from);
                    if (d < playerDist) {
                        playerDist = d;
                        hitPlayer = candidate;
                        playerHitPos = r.getHitPosition();
                    }
                }
            }
        }

        double blockDist = Double.MAX_VALUE;
        Vector blockHitPos = null;
        if (distance > 1.0E-6) {
            RayTraceResult br = world.rayTraceBlocks(
                    lastLocation, direction, distance, FluidCollisionMode.NEVER, true);
            if (br != null) {
                blockDist = br.getHitPosition().distance(from);
                blockHitPos = br.getHitPosition();
            }
        }

        if (hitPlayer != null && playerDist <= blockDist) {
            explodeAt(playerHitPos.toLocation(world), hitPlayer);
            return;
        }
        if (blockHitPos != null) {
            explodeAt(blockHitPos.toLocation(world), null);
            return;
        }

        lastLocation = now.clone();
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