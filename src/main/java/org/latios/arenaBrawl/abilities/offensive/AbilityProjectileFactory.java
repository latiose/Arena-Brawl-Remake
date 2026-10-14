
package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.AbilityItemKeys;


public class AbilityProjectileFactory {

    private static final double BASE_SPEED = 1.5;
    private static final double FIREBALL_BASE_SPEED = 1.0;
    private static final int MAX_CONSTANT_VELOCITY_TICKS = 20 * 15;

    public static <T extends Projectile> T launchAoe(
            Player shooter, Class<T> projectileType, double damage, String abilityName,
            double radius, double speedMultiplier) {

        T projectile = shooter.launchProjectile(projectileType);
        Vector direction = shooter.getEyeLocation().getDirection();

        if (projectile instanceof Fireball) {
            Vector velocity = direction.clone().multiply(FIREBALL_BASE_SPEED * speedMultiplier);
            projectile.setVelocity(velocity);
            keepConstantVelocity(projectile, velocity);
        } else {
            projectile.setVelocity(direction.clone().multiply(BASE_SPEED * speedMultiplier));
        }

        tagDamage(projectile, damage, abilityName);
        projectile.getPersistentDataContainer().set(
                AbilityItemKeys.PROJECTILE_AOE_RADIUS, PersistentDataType.DOUBLE, radius);
        projectile.getPersistentDataContainer().set(
                AbilityItemKeys.PROJECTILE_DIRECT_HIT, PersistentDataType.BYTE, (byte) 1);
        return projectile;
    }

    private static void keepConstantVelocity(Projectile projectile, Vector velocity) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!projectile.isValid() || ++ticks > MAX_CONSTANT_VELOCITY_TICKS) {
                    cancel();
                    return;
                }
                projectile.setVelocity(velocity);
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 1L, 1L);
    }


    private static void tagDamage(Projectile projectile, double damage, String abilityName) {
        projectile.getPersistentDataContainer().set(
                AbilityItemKeys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE, damage
        );
        projectile.getPersistentDataContainer().set(
                AbilityItemKeys.PROJECTILE_SOURCE_ABILITY, PersistentDataType.STRING, abilityName
        );
    }

}