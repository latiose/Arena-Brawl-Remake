// general/ProjectileAoeListener.java
package org.latios.arenaBrawl.general;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
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

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player shooter)) return;

        Double radius = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_AOE_RADIUS, PersistentDataType.DOUBLE
        );
        if (radius == null) return;

        Double damage = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE
        );
        String abilityName = projectile.getPersistentDataContainer().get(
                AbilityItemKeys.PROJECTILE_SOURCE_ABILITY, PersistentDataType.STRING
        );

        double finalDamage = damage != null ? damage : 0.0;
        String finalAbilityName = abilityName != null ? abilityName : "Unknown";

        Set<Player> alreadyHit = new HashSet<>();

        // Direct hit: if the projectile collided with an entity directly, always damage it
        // first, regardless of AoE radius — this is what was being missed before.
        if (event.getHitEntity() instanceof Player directVictim
                && teamManager.isEnemy(shooter, directVictim)) {
            combatService.applyAbilityDamage(shooter, directVictim, finalDamage, finalAbilityName);
            alreadyHit.add(directVictim);
        }

        // AoE: damage everyone else within radius of the impact point
        for (Entity nearby : projectile.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Player target
                    && teamManager.isEnemy(shooter, target)
                    && !alreadyHit.contains(target)) {
                combatService.applyAbilityDamage(shooter, target, finalDamage, finalAbilityName);
            }
        }

        projectile.remove();
    }
}