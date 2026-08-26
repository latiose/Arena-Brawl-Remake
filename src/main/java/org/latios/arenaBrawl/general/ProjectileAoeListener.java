
package org.latios.arenaBrawl.general;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.latios.arenaBrawl.team.TeamManager;


/**
 * Resolves area-of-effect damage for ANY projectile tagged with PROJECTILE_AOE_RADIUS,
 * regardless of its entity type (Fireball, Arrow, Snowball, custom projectiles, etc.).
 * Abilities only need to tag their projectile's PersistentDataContainer; no new listener
 * is needed per ability.
 */
public class ProjectileAoeListener implements Listener {

    private final TeamManager teamManager;
    private final CombatService combatService;

    public ProjectileAoeListener(TeamManager teamManager, CombatService combatService) {
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player shooter)) return;

        Double radius = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_AOE_RADIUS, PersistentDataType.DOUBLE
        );
        if (radius == null) return; // not an AoE-tagged projectile, let CombatListener handle direct hits

        Double damage = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE
        );
        String abilityName = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_SOURCE_ABILITY, PersistentDataType.STRING
        );

        double finalDamage = damage != null ? damage : 0.0;
        String finalAbilityName = abilityName != null ? abilityName : "Unknown";

        for (Entity nearby : projectile.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Player target && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, finalDamage, finalAbilityName);
            }
        }

        projectile.remove();
    }
}