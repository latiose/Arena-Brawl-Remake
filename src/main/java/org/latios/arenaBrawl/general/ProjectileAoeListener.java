package org.latios.arenaBrawl.general;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.general.AbilityItemKeys;

import java.util.HashSet;
import java.util.Set;

public class ProjectileAoeListener implements Listener {

    private final TeamManager teamManager;
    private final CombatService combatService;

    public ProjectileAoeListener(TeamManager teamManager, CombatService combatService) {
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    public void trackDirectHit(Projectile projectile, Player shooter) {
        if (!projectile.getPersistentDataContainer().has(
                AbilityItemKeys.PROJECTILE_DIRECT_HIT, PersistentDataType.BYTE)) {
            return;
        }

        new BukkitRunnable() {
            private org.bukkit.Location previous = projectile.getLocation();

            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    cancel();
                    return;
                }

                org.bukkit.Location current = projectile.getLocation();
                org.bukkit.util.Vector movement = current.toVector().subtract(previous.toVector());
                double distance = movement.length();
                if (distance > 0.0) {
                    RayTraceResult result = current.getWorld().rayTrace(
                            previous, movement.normalize(), distance,
                            org.bukkit.FluidCollisionMode.NEVER, true, 0.2,
                            entity -> entity instanceof Player target
                                    && !target.equals(shooter)
                                    && !target.isDead()
                                    && teamManager.isEnemy(shooter, target)
                    );
                    if (result != null && result.getHitEntity() instanceof Player target) {
                        detonate(projectile, shooter, result.getHitPosition().toLocation(current.getWorld()), target);
                        cancel();
                        return;
                    }
                }
                previous = current;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
    }

    @EventHandler
    public void onProjectileDeflected(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile)
                || !(projectile.getShooter() instanceof Player)
                || !(event.getDamager() instanceof Player)) {
            return;
        }
        if (projectile.getPersistentDataContainer().has(
                AbilityItemKeys.PROJECTILE_AOE_RADIUS, PersistentDataType.DOUBLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player shooter)) return;

        Double radius = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_AOE_RADIUS, PersistentDataType.DOUBLE
        );
        if (radius == null) return;

        Player directTarget = event.getHitEntity() instanceof Player player ? player : null;
        org.bukkit.Location center = directTarget != null
                ? directTarget.getBoundingBox().getCenter().toLocation(projectile.getWorld())
                : projectile.getLocation();
        detonate(projectile, shooter, center, directTarget);
    }

    private void detonate(Projectile projectile, Player shooter, org.bukkit.Location center,
                          Player directTarget) {
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.FLAME, center, 30, 0.45, 0.45, 0.45, 0.08);
        center.getWorld().spawnParticle(Particle.SMOKE, center, 20, 0.35, 0.35, 0.35, 0.04);

        Double radius = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_AOE_RADIUS, PersistentDataType.DOUBLE
        );
        Double damage = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE
        );
        String abilityName = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_SOURCE_ABILITY, PersistentDataType.STRING
        );

        double finalDamage = damage != null ? damage : 0.0;
        String finalAbilityName = abilityName != null ? abilityName : "Unknown";

        Set<Player> alreadyHit = new HashSet<>();

        boolean directHitEnabled = projectile.getPersistentDataContainer().has(
                AbilityItemKeys.PROJECTILE_DIRECT_HIT, PersistentDataType.BYTE
        );
        if (directHitEnabled && directTarget != null
                && !directTarget.isDead()
                && teamManager.isEnemy(shooter, directTarget)) {
            combatService.applyAbilityDamage(shooter, directTarget, finalDamage, finalAbilityName, center);
            alreadyHit.add(directTarget);
        }

        // AoE: damage everyone else within radius of the impact point
        for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (nearby instanceof Player target
                    && teamManager.isEnemy(shooter, target)
                    && !alreadyHit.contains(target)) {
                combatService.applyAbilityDamage(shooter, target, finalDamage, finalAbilityName, center);
            }
        }

        projectile.remove();
    }
}