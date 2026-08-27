package org.latios.arenaBrawl.general;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.team.TeamManager;

public class TrackedProjectileTask extends BukkitRunnable {

    private final Projectile projectile;
    private final Player shooter;
    private final double damage;
    private final double aoeRadius;
    private final String abilityName;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public TrackedProjectileTask(Projectile projectile, Player shooter, double damage, double aoeRadius,
                                 String abilityName, TeamManager teamManager, CombatService combatService) {
        this.projectile = projectile;
        this.shooter = shooter;
        this.damage = damage;
        this.aoeRadius = aoeRadius;
        this.abilityName = abilityName;
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public void run() {
        if (projectile.isDead() || !projectile.isValid()) {
            cancel();
            return;
        }
        for (Player candidate : projectile.getWorld().getPlayers()) {
            if (candidate.equals(shooter)) continue;
            if (!teamManager.isEnemy(shooter, candidate)) continue;

            if (candidate.getBoundingBox().expand(0.5).contains(projectile.getLocation().toVector())) {
                explode(candidate);
                return;
            }
        }

        if (projectile.isOnGround()) {
            explodeAtLocation();
        }
    }

    private void explode(Player directHitVictim) {
        cancel();
        combatService.applyAbilityDamage(shooter, directHitVictim, damage, abilityName, projectile.getLocation());

        for (Entity nearby : projectile.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && !target.equals(directHitVictim)
                    && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, damage, abilityName);
            }
        }

        projectile.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, projectile.getLocation(), 1);
        projectile.getWorld().playSound(projectile.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
        projectile.remove();
    }

    private void explodeAtLocation() {
        cancel();

        for (Entity nearby : projectile.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, damage, abilityName);
            }
        }

        projectile.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, projectile.getLocation(), 1);
        projectile.remove();
    }
}