package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

/**
 * Ticks a placed Shock Mine. It hums in place until an enemy walks close,
 * then detonates for area damage and a stun.
 */
public class ShockMineTask extends BukkitRunnable {

    private static final int INTERVAL_TICKS = 2;

    private final Player owner;
    private final Location location;
    private final double damage;
    private final double triggerRadius;
    private final double blastRadius;
    private final long stunDurationMs;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final Runnable onEnd;

    private int ticks = 0;
    private boolean ended = false;

    public ShockMineTask(Player owner, Location location, double damage, double triggerRadius,
                         double blastRadius, long stunDurationMs,
                         TeamManager teamManager, CombatService combatService, DebuffManager debuffManager,
                         Runnable onEnd) {
        this.owner = owner;
        this.location = location;
        this.damage = damage;
        this.triggerRadius = triggerRadius;
        this.blastRadius = blastRadius;
        this.stunDurationMs = stunDurationMs;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.onEnd = onEnd;
    }

    private void notifyEnd() {
        if (ended) return;
        ended = true;
        if (onEnd != null) onEnd.run();
    }

    /**
     * Removes this mine without detonating it. Used when the owner has reached the
     * mine limit and the oldest mine is replaced by a newly placed one.
     */
    public void deactivate() {
        if (isCancelled()) return;
        fade();
    }

    @Override
    public void run() {
        ticks += INTERVAL_TICKS;

        if (!owner.isOnline()) {
            fade();
            return;
        }

        double pulse = 0.4 + 0.3 * Math.sin(ticks * 0.4);
        location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 4,
                pulse / 3, 0.15, pulse / 3, 0.01);
        location.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0.05, 0.1, 0.05, 0);

        if (ticks % 20 == 0) {
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.8f);
        }

        for (Entity nearby : location.getWorld().getNearbyEntities(location, triggerRadius, triggerRadius, triggerRadius)) {
            if (nearby instanceof Player target && teamManager.isEnemy(owner, target) && !target.isDead()) {
                detonate();
                return;
            }
        }
    }

    private void detonate() {
        notifyEnd();
        cancel();

        for (Entity nearby : location.getWorld().getNearbyEntities(location, blastRadius, blastRadius, blastRadius)) {
            if (nearby instanceof Player target && teamManager.isEnemy(owner, target) && !target.isDead()) {
                combatService.applyAbilityDamage(owner, target, damage, "Shock Mine", location);
                debuffManager.tryApply(owner, target, DebuffType.IMMOBILIZE, stunDurationMs);
            }
        }

        location.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, location, 1, 0, 0, 0, 0);
        location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 40, 0.4, 0.5, 0.4, 0.2);
        location.getWorld().playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.4f);
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.6f);
    }

    private void fade() {
        notifyEnd();
        cancel();
        location.getWorld().spawnParticle(Particle.SMOKE, location, 8, 0.2, 0.2, 0.2, 0.02);
    }
}
