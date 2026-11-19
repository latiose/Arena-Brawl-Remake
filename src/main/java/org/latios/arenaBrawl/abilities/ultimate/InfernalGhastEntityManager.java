package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.BaseMinionManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.UUID;

/**
 * Manager for the Infernal Ghast ultimate. The ghast keeps its vanilla AI
 * (movement, look and the normal charge/shoot sounds) and is only nudged
 * toward a point above its target so it keeps hunting from the sky. Its
 * vanilla fireballs are turned into a flat 50 damage hit that cannot be
 * reflected or deflected.
 */
public class InfernalGhastEntityManager extends BaseMinionManager {

    private final double fireballDamage;
    private final double aoeRadius;
    private final double spawnHeight;
    private final double hoverHeight;
    private final double moveSpeed;
    private final double teleportDistance;

    public InfernalGhastEntityManager(TeamManager teamManager, CombatService combatService,
                                      AbilityConfig config) {
        super(teamManager, combatService,
                config.getLong("teleport-timeout-millis", Long.MAX_VALUE),
                config.getLong("attack-cooldown-millis", 1_000L));

        this.fireballDamage = config.getDouble("fireball-damage", 50.0);
        this.aoeRadius = config.getDouble("aoe-radius", 2.5);
        this.spawnHeight = config.getDouble("spawn-height", 10.0);
        this.hoverHeight = config.getDouble("hover-height", 6.0);
        this.moveSpeed = config.getDouble("move-speed", 0.5);
        this.teleportDistance = config.getDouble("teleport-distance", 30.0);
    }

    public Ghast summonGhast(Player owner, Player preferredTarget) {
        Location spawn = findSpawn(owner);

        Ghast ghast = owner.getWorld().spawn(spawn, Ghast.class, entity -> {
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);
            // Vanilla AI: it flies with its own move control and shoots its
            // normal fireballs (with the normal charge/shoot sounds).
            entity.setAI(true);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setSilent(false);

            // Paper caps MAX_HEALTH at 1024; the real immortality comes from
            // the invulnerability flag and the damage listener.
            var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) maxHealth.setBaseValue(1024.0);
            entity.setHealth(1024.0);
        });

        EntityCleanupUtils.markAsArenaEntity(ghast);
        // "Inmortal": huge hit count plus invulnerability. It only leaves when
        // the match ends or the manager is cleared.
        registerControlledEntity(ghast, owner, Integer.MAX_VALUE);
        acquireTarget(ghast, owner, preferredTarget);

        World world = spawn.getWorld();
        world.spawnParticle(Particle.EXPLOSION, spawn, 3, 0.8, 0.8, 0.8, 0.0);
        world.spawnParticle(Particle.FLAME, spawn, 80, 1.2, 1.2, 1.2, 0.05);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, spawn, 40, 1.4, 1.4, 1.4, 0.02);
        world.playSound(spawn, Sound.ENTITY_GHAST_SCREAM, 3.0f, 0.7f);

        return ghast;
    }

    /**
     * Called by {@link InfernalGhastAI} every tick. Only re-acquires a
     * target when the current one died or left: otherwise it is kept forever.
     * Vanilla AI does the actual flying and shooting.
     */
    public void tick(Ghast ghast) {
        Player target = getCurrentTarget(ghast);

        if (target == null) {
            acquireTarget(ghast);
            target = getCurrentTarget(ghast);
        }

        if (target == null) {
            ghast.setTarget(null);
            ghast.getPathfinder().stopPathfinding();
            ghast.setVelocity(ghast.getVelocity().multiply(0.6));
            return;
        }

        ghast.setTarget(target);

        // Steer it to a point above the enemy. The vanilla AI still does the
        // shooting (and its normal sounds); we only correct the flight so it
        // never drifts away or loses the target.
        Location hover = hoverPoint(target);
        Vector delta = hover.toVector().subtract(ghast.getLocation().toVector());
        double distance = delta.length();

        if (distance > teleportDistance) {
            // If it ever falls behind, catch up instantly instead of losing
            // the target.
            ghast.teleport(hover);
            return;
        }

        if (distance > 1.0) {
            double speed = Math.min(moveSpeed, 0.08 + distance * 0.05);
            ghast.setVelocity(delta.normalize().multiply(speed));
        } else {
            ghast.setVelocity(ghast.getVelocity().multiply(0.5));
        }
    }

    private Location hoverPoint(Player target) {
        Location hover = target.getLocation().clone().add(0, hoverHeight, 0);

        int maxY = hover.getWorld().getMaxHeight() - 5;
        if (hover.getY() > maxY) hover.setY(maxY);

        // If the chosen spot is inside terrain, drop it toward the enemy
        // instead of climbing into the ceiling.
        double minY = target.getLocation().getY() + 1.5;
        for (int i = 0; i < 12 && hover.getY() > minY && hover.getBlock().getType().isSolid(); i++) {
            hover.subtract(0, 1, 0);
        }
        return hover;
    }

    /**
     * Applies the custom fireball damage. Called from the projectile listener
     * when one of this ghast's (vanilla) fireballs hits something.
     */
    public void handleFireballImpact(Ghast ghast, Player directHit, Location impact) {
        if (ghast == null || impact == null) return;
        if (!isControlledEntity(ghast)) return;

        Player owner = ownerOf(ghast);
        if (owner == null || combatService == null) return;

        Player victim;
        if (directHit != null && isValidEnemy(ghast, directHit)) {
            victim = directHit;
        } else {
            victim = findNearestEnemy(ghast, impact, aoeRadius);
        }
        if (victim == null) return;

        // Unreflectable: Counter / Reflect Damage must not turn this back on the
        // ghast or its owner.
        combatService.applyUnreflectableMinionDamage(owner, victim, fireballDamage,
                "Infernal Ghast", impact.clone().add(0, 1.0, 0));
        victim.setFireTicks(0);
    }

    private Player ownerOf(Ghast ghast) {
        UUID ownerId = ownerOf.get(ghast.getUniqueId());
        return ownerId != null ? Bukkit.getPlayer(ownerId) : null;
    }

    private boolean isValidEnemy(Ghast ghast, Player candidate) {
        return candidate != null
                && candidate.isOnline()
                && !candidate.isDead()
                && candidate.getGameMode() != GameMode.SPECTATOR
                && isEnemyOf(ghast, candidate);
    }

    private Player findNearestEnemy(Ghast ghast, Location impact, double radius) {
        Player nearest = null;
        double closest = radius * radius;

        for (Player candidate : ghast.getWorld().getPlayers()) {
            if (!isValidEnemy(ghast, candidate)) continue;

            double distance = candidate.getLocation().distanceSquared(impact);
            if (distance <= closest) {
                closest = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private Location findSpawn(Player owner) {
        Location base = owner.getLocation().clone().add(0, spawnHeight, 0);

        int maxY = base.getWorld().getMaxHeight() - 6;
        if (base.getY() > maxY) base.setY(maxY);

        for (int i = 0; i < 24 && base.getBlock().getType().isSolid(); i++) {
            base.add(0, 1, 0);
        }
        return base;
    }

    @Override
    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        // The ghast never melees; all of its damage comes from fireballs.
    }

    @Override
    protected Sound getHurtSound() {
        return Sound.ENTITY_GHAST_HURT;
    }

    @Override
    protected Sound getDeathSound() {
        return Sound.ENTITY_GHAST_DEATH;
    }
}
