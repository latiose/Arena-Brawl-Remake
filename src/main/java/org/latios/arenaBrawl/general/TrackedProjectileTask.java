package org.latios.arenaBrawl.general;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.offensive.ProjectileImpactEffect;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.concurrent.ThreadLocalRandom;

public class TrackedProjectileTask extends BukkitRunnable {

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
    }

    public TrackedProjectileTask(Projectile projectile, Player shooter, double damage, double aoeRadius,
                                 String abilityName, TeamManager teamManager, CombatService combatService,
                                 ProjectileImpactEffect impactEffect) {
        this(projectile, shooter, damage, aoeRadius, abilityName, teamManager, combatService, null, null, 0, 0, impactEffect);
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
        if (debuffManager != null && debuffChance > 0) {
            if (ThreadLocalRandom.current().nextDouble() < debuffChance) {
                debuffManager.tryApply(directHitVictim, debuffType, debuffDuration);
            }
        }
        combatService.applyAbilityDamage(shooter, directHitVictim, damage, abilityName, projectile.getLocation());

        for (Entity nearby : projectile.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && !target.equals(directHitVictim)
                    && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, damage, abilityName);
            }
        }

        playImpactEffects();
        projectile.remove();
    }

    private void explodeAtLocation() {
        cancel();

        for (Entity nearby : projectile.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, damage, abilityName);
            }
        }

        playImpactEffects();
        projectile.remove();
    }

    private void playImpactEffects() {
        projectile.getWorld().spawnParticle(
                impactEffect.particle(),
                projectile.getLocation(),
                impactEffect.particleCount(),
                0.2, 0.2, 0.2, 0.05
        );
        projectile.getWorld().playSound(
                projectile.getLocation(),
                impactEffect.sound(),
                impactEffect.volume(),
                impactEffect.pitch()
        );
    }
}