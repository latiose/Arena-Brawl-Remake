package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.general.AbilityItemKeys;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A particle-only vertical screen that destroys enemy projectiles crossing it.
 * <p>
 * The screen has no solid blocks, so it never blocks movement or line of sight. It is registered
 * as a structure so demolition abilities and match cleanup can remove it.
 */
public class AegisScreenStructure extends PlacedStructure implements DestructibleStructure {

    private final Plugin plugin;
    private final TeamManager teamManager;
    private final Location center;
    private final Vector normal;
    private final Vector widthAxis;
    private final double width;
    private final double height;
    private final long durationMillis;
    private final int meleeHitsToDestroy;

    private final Map<UUID, Location> lastProjectilePositions = new HashMap<>();

    private BukkitTask task;
    private int remainingHits;
    private int ticks;
    private boolean expired;

    public AegisScreenStructure(Player owner, Location center, Vector normal, double width, double height,
                                Plugin plugin, TeamManager teamManager, long durationMillis, int hitsToDestroy) {
        super(owner, center.clone().subtract(0, height / 2.0, 0));
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.center = center.clone();
        this.normal = normal.clone().setY(0).normalize();
        this.widthAxis = new Vector(this.normal.getZ(), 0, -this.normal.getX()).normalize();
        this.width = width;
        this.height = height;
        this.durationMillis = durationMillis;
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;

        ProjectileInterceptionService.register(this);
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickScreen();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void tickScreen() {
        if (System.currentTimeMillis() - getPlacedAt() >= durationMillis) {
            expired = true;
            cancelTask();
            return;
        }

        ticks++;
        if (ticks % 2 == 0) {
            drawScreen();
        }
        destroyEnemyProjectiles();
    }

    private void drawScreen() {
        for (int ix = -1; ix <= 1; ix++) {
            for (int iy = -1; iy <= 1; iy++) {
                double lateral = ix * (width / 2.0) * 0.8;
                double vertical = iy * (height / 2.0) * 0.8;
                Location point = center.clone().add(widthAxis.clone().multiply(lateral)).add(0, vertical, 0);

                point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.04, 0.04, 0.04, 0.0);
                if (ticks % 4 == 0) {
                    point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.08, 0.08, 0.08, 0.0);
                }
            }
        }

        if (ticks % 20 == 0) {
            center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.4f);
        }
    }

    private void destroyEnemyProjectiles() {
        double radius = Math.max(width, height) / 2.0 + 4.0;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Projectile projectile)) continue;
            if (projectile.isDead() || !isEnemyProjectile(projectile)) continue;

            Location current = projectile.getLocation();
            Location previous = lastProjectilePositions.put(projectile.getUniqueId(), current.clone());

            if (previous == null) {
                if (isInsideScreen(current)) {
                    destroyProjectile(projectile);
                }
                continue;
            }

            if (crossesScreen(previous, current) || isInsideScreen(current)) {
                destroyProjectile(projectile);
            }
        }
    }

    /**
     * Called by {@link ProjectileInterceptionService} for projectiles that are not
     * Bukkit {@link Projectile} entities (armor-stand launchers, item displays,
     * living entities, particle beams...).
     */
    public boolean tryIntercept(Player shooter, Location from, Location to) {
        if (expired) return false;
        if (!isEnemyShooter(shooter)) return false;
        if (center.getWorld() == null || from.getWorld() == null || to.getWorld() == null
                || !center.getWorld().equals(from.getWorld())
                || !center.getWorld().equals(to.getWorld())) {
            return false;
        }

        Location impact = intersection(from, to);
        if (impact == null && isInsideScreen(to)) {
            impact = to.clone();
        }
        if (impact == null) return false;

        playInterceptEffects(impact);
        return true;
    }

    /** Point test for hitscan / instant beam abilities. */
    public boolean blocksPoint(Player shooter, Location point) {
        if (expired) return false;
        if (!isEnemyShooter(shooter)) return false;
        if (center.getWorld() == null || point.getWorld() == null
                || !center.getWorld().equals(point.getWorld())) {
            return false;
        }
        if (!isInsideScreen(point)) return false;

        playInterceptEffects(point);
        return true;
    }

    private boolean isEnemyShooter(Player shooter) {
        if (shooter == null) return false;
        Player owner = getOwner();
        return owner != null && teamManager.isEnemy(owner, shooter);
    }

    private boolean isEnemyProjectile(Projectile projectile) {
        Player shooter = resolveShooter(projectile);
        return shooter != null && isEnemyShooter(shooter);
    }

    /**
     * Resolves the owning player for a Bukkit projectile. Most ability projectiles
     * use the player as their shooter, but skeleton arrows (and any future mob
     * projectile) store the owner UUID in the persistent data container.
     */
    private Player resolveShooter(Projectile projectile) {
        ProjectileSource source = projectile.getShooter();
        if (source instanceof Player player) {
            return player;
        }

        String ownerId = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_OWNER, PersistentDataType.STRING);
        if (ownerId == null) return null;

        try {
            return Bukkit.getPlayer(java.util.UUID.fromString(ownerId));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean crossesScreen(Location from, Location to) {
        return intersection(from, to) != null;
    }

    private Location intersection(Location from, Location to) {
        double fromDistance = from.toVector().subtract(center.toVector()).dot(normal);
        double toDistance = to.toVector().subtract(center.toVector()).dot(normal);

        if (fromDistance * toDistance > 0) return null;

        double denominator = fromDistance - toDistance;
        if (Math.abs(denominator) < 1.0E-6) return null;

        double t = fromDistance / denominator;
        Vector fromVector = from.toVector();
        Vector toVector = to.toVector();
        Vector point = fromVector.clone().add(toVector.clone().subtract(fromVector).multiply(t));
        Location intersection = point.toLocation(center.getWorld());
        return isInsideScreen(intersection) ? intersection : null;
    }

    private boolean isInsideScreen(Location point) {
        Vector delta = point.toVector().subtract(center.toVector());
        double distance = delta.dot(normal);
        double lateral = delta.dot(widthAxis);
        double vertical = delta.getY();

        return Math.abs(distance) <= 0.6
                && Math.abs(lateral) <= width / 2.0 + 0.4
                && Math.abs(vertical) <= height / 2.0 + 0.4;
    }

    private void destroyProjectile(Projectile projectile) {
        Location at = projectile.getLocation();
        lastProjectilePositions.remove(projectile.getUniqueId());
        ProjectileInterceptionService.markIntercepted(projectile.getUniqueId());
        projectile.remove();

        playInterceptEffects(at);
    }

    private void playInterceptEffects(Location at) {
        at.getWorld().spawnParticle(Particle.END_ROD, at, 12, 0.2, 0.2, 0.2, 0.05);
        at.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, at, 8, 0.2, 0.2, 0.2, 0.1);
        at.getWorld().playSound(at, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 1.6f);
    }

    @Override
    public boolean tick() {
        return System.currentTimeMillis() - getPlacedAt() >= durationMillis;
    }

    @Override
    public void remove() {
        expired = true;
        ProjectileInterceptionService.unregister(this);
        cancelTask();
        lastProjectilePositions.clear();

        if (center.getWorld() != null) {
            center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 20,
                    width / 2.0, height / 2.0, width / 2.0, 0.1);
            center.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.5f);
        }
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public boolean registerHit(Player attacker) {
        if (!canHit(attacker)) return false;

        remainingHits--;
        center.getWorld().spawnParticle(Particle.CRIT, center, 12, width / 3.0, height / 3.0, width / 3.0);
        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.2f);

        return remainingHits <= 0;
    }

    @Override
    public boolean canHit(Player attacker) {
        Player owner = getOwner();
        if (owner != null && !teamManager.isEnemy(owner, attacker)) {
            return false;
        }
        return true;
    }

    @Override
    public int getHealthPercentage() {
        return (int) Math.ceil(((double) remainingHits / meleeHitsToDestroy) * 100);
    }

    @Override
    public String getStructureName() {
        return "Aegis Screen";
    }

    @Override
    public boolean isHitByRay(Player player, double maxDistance) {
        Vector eye = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection().normalize();

        double denominator = direction.dot(normal);
        if (Math.abs(denominator) < 1.0E-6) return false;

        double t = center.toVector().subtract(eye).dot(normal) / denominator;
        if (t <= 0 || t > maxDistance) return false;

        // Do not register hits through solid terrain: the screen must be the first thing
        // in the line of sight (it has no blocks of its own, so getTargetBlockExact only
        // reports real terrain).
        Block target = player.getTargetBlockExact((int) Math.ceil(maxDistance));
        if (target != null) {
            double targetDistance = player.getEyeLocation().distance(
                    target.getLocation().add(0.5, 0.5, 0.5));
            if (t > targetDistance) return false;
        }

        Vector point = eye.clone().add(direction.clone().multiply(t));
        Vector delta = point.subtract(center.toVector());
        double lateral = delta.dot(widthAxis);
        double vertical = delta.getY();

        return Math.abs(lateral) <= width / 2.0 + 0.3
                && Math.abs(vertical) <= height / 2.0 + 0.3;
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return List.of();
    }

    @Override
    public List<Location> getHitLocations() {
        List<Location> hits = new ArrayList<>();

        for (int ix = -1; ix <= 1; ix++) {
            for (int iy = -1; iy <= 1; iy++) {
                double lateral = ix * (width / 2.0) * 0.8;
                double vertical = iy * (height / 2.0) * 0.8;
                hits.add(center.clone().add(widthAxis.clone().multiply(lateral)).add(0, vertical, 0));
            }
        }

        return hits;
    }
}
