
package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;
import org.latios.arenaBrawl.general.AbilityItemKeys;


public class AbilityProjectileFactory {

    /**
     * Launches a projectile of the given type and tags it as a direct-hit ability projectile
     * (no AoE — only the entity it directly collides with takes damage, resolved via CombatListener).
     */
    public static <T extends Projectile> T launchDirectHit(
            Player shooter, Class<T> projectileType, double damage, String abilityName) {

        T projectile = shooter.launchProjectile(projectileType);
        tagDamage(projectile, damage, abilityName);
        return projectile;
    }

    /**
     * Launches a projectile of the given type and tags it as an area-of-effect ability projectile
     * (resolved via ProjectileAoeListener on impact — damages every enemy within radius).
     */
    public static <T extends Projectile> T launchAoe(
            Player shooter, Class<T> projectileType, double damage, String abilityName, double radius) {

        T projectile = shooter.launchProjectile(projectileType);
        tagDamage(projectile, damage, abilityName);
        projectile.getPersistentDataContainer().set(
                AbilityItemKeys.PROJECTILE_AOE_RADIUS, PersistentDataType.DOUBLE, radius
        );
        return projectile;
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