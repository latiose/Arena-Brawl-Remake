package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Makes the Infernal Ghast truly immortal and turns its vanilla fireballs into
 * a flat 50-damage hit that cannot be deflected or reflected. The ghast still
 * uses its normal AI to move and shoot (normal charge/shoot sounds included).
 */
public class InfernalGhastListener implements Listener {

    private final InfernalGhastEntityManager entityManager;

    public InfernalGhastListener(InfernalGhastEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** The ghast itself is invulnerable; this also covers direct /kill style hits. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGhastDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Ghast ghast)) return;
        if (!entityManager.isControlledEntity(ghast)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireballDamage(EntityDamageByEntityEvent event) {
        // Hitting the fireball with melee used to deflect it: never allow it.
        if (event.getEntity() instanceof LargeFireball fireball && isOurFireball(fireball)) {
            event.setCancelled(true);
            return;
        }

        // Vanilla direct hit damage is replaced by the custom flat damage that is
        // applied in onProjectileHit.
        if (event.getDamager() instanceof LargeFireball fireball && isOurFireball(fireball)) {
            event.setCancelled(true);
        }
    }

    /** Suppresses the vanilla explosion so only the flat damage applies. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof LargeFireball fireball && isOurFireball(fireball)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof LargeFireball fireball)) return;
        if (!isOurFireball(fireball)) return;

        Ghast ghast = fireball.getShooter() instanceof Ghast shooter ? shooter : null;
        Entity hit = event.getHitEntity();
        Player directHit = hit instanceof Player player ? player : null;
        Location impact = fireball.getLocation().clone();

        entityManager.handleFireballImpact(ghast, directHit, impact);

        World world = impact.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.EXPLOSION, impact, 2, 0.25, 0.25, 0.25, 0.0);
            world.spawnParticle(Particle.FLAME, impact, 30, 0.6, 0.6, 0.6, 0.05);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, impact, 15, 0.5, 0.5, 0.5, 0.02);
            world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        }

        fireball.remove();
    }

    /**
     * Any fireball shot by a controlled ghast counts as ours, so the vanilla
     * AI can keep shooting while the damage stays custom.
     */
    private boolean isOurFireball(LargeFireball fireball) {
        return fireball.getShooter() instanceof Ghast ghast
                && entityManager.isControlledEntity(ghast);
    }
}
