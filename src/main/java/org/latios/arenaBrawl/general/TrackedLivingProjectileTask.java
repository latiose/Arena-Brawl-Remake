package org.latios.arenaBrawl.general;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.Set;

public class TrackedLivingProjectileTask extends BukkitRunnable {

    private static final int MAX_LIFETIME_TICKS = 80;

    private final LivingEntity entity;
    private final Player shooter;
    private final Vector velocity;
    private final double speed;
    private final double damage;
    private final double hitRadius;
    private final double aoeRadius;

    private final String abilityName;
    private final TeamManager teamManager;
    private final CombatService combatService;

    private final Particle impactParticle;
    private final int particleCount;
    private final Sound impactSound;
    private final float soundVolume;
    private final float soundPitch;

    private int ticksElapsed = 0;

    public TrackedLivingProjectileTask(LivingEntity entity, Player shooter, Vector direction, double speed,
                                       double damage, double hitRadius, double aoeRadius,
                                       String abilityName, TeamManager teamManager, CombatService combatService,
                                       Particle impactParticle, int particleCount,
                                       Sound impactSound, float soundVolume, float soundPitch) {
        this.entity = entity;
        this.shooter = shooter;
        this.speed = speed;
        this.velocity = direction.clone().normalize().multiply(speed);
        this.damage = damage;
        this.hitRadius = hitRadius;
        this.aoeRadius = aoeRadius;
        this.abilityName = abilityName;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.impactParticle = impactParticle;
        this.particleCount = particleCount;
        this.impactSound = impactSound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
    }

    @Override
    public void run() {
        if (entity.isDead() || !entity.isValid() || ticksElapsed >= MAX_LIFETIME_TICKS) {
            explodeAtLocation();
            return;
        }

        RayTraceResult blockHit = entity.getWorld().rayTraceBlocks(
                entity.getLocation(),
                velocity.clone().normalize(),
                speed,
                FluidCollisionMode.NEVER,
                true
        );

        if (blockHit != null && blockHit.getHitBlock() != null) {
            explodeAtLocation();
            return;
        }

        entity.setVelocity(velocity);
        ticksElapsed++;

        for (Player candidate : entity.getWorld().getPlayers()) {
            if (candidate.equals(shooter)) continue;
            if (!teamManager.isEnemy(shooter, candidate)) continue;

            double distance = candidate.getLocation().distance(entity.getLocation());
            if (distance <= hitRadius) {
                explode(candidate);
                return;
            }
        }
    }

    private void explode(Player directHitVictim) {
        cancel();

        Set<Player> alreadyHit = new HashSet<>();
        combatService.applyAbilityDamage(shooter, directHitVictim, damage, abilityName, entity.getLocation());
        alreadyHit.add(directHitVictim);

        applyAoeDamage(alreadyHit);
        spawnImpactEffects();
        entity.remove();
    }

    private void explodeAtLocation() {
        cancel();

        applyAoeDamage(new HashSet<>());
        spawnImpactEffects();
        entity.remove();
    }

    private void applyAoeDamage(Set<Player> alreadyHit) {
        if (aoeRadius <= 0) return;

        for (Entity nearby : entity.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target
                    && !alreadyHit.contains(target)
                    && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, damage, abilityName, entity.getLocation());
                alreadyHit.add(target);
            }
        }
    }

    private void spawnImpactEffects() {
        if (impactParticle != null && particleCount > 0) {
            entity.getWorld().spawnParticle(impactParticle, entity.getLocation(), particleCount);
        }
        if (impactSound != null) {
            entity.getWorld().playSound(entity.getLocation(), impactSound, soundVolume, soundPitch);
        }
    }
}