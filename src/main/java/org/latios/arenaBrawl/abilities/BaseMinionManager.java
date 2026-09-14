package org.latios.arenaBrawl.abilities;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseMinionManager {

    protected final Map<UUID, Integer> remainingHits = new HashMap<>();
    protected final Map<UUID, UUID> ownerOf = new HashMap<>();
    protected final Map<UUID, UUID> currentTarget = new HashMap<>();
    protected final Map<UUID, Long> lastLandedHitAt = new HashMap<>();
    protected final Map<UUID, Long> lastAttackAt = new HashMap<>();

    protected final TeamManager teamManager;
    protected CombatService combatService;
    private final long teleportTimeoutMillis;
    private final long attackCooldownMillis;

    public BaseMinionManager(TeamManager teamManager, CombatService combatService, long teleportTimeoutMillis, long attackCooldownMillis) {
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.teleportTimeoutMillis = teleportTimeoutMillis;
        this.attackCooldownMillis = attackCooldownMillis;
    }

    protected void registerControlledEntity(LivingEntity entity, UUID ownerId, int hitCount) {
        UUID id = entity.getUniqueId();
        remainingHits.put(id, hitCount);
        ownerOf.put(id, ownerId);
        lastLandedHitAt.put(id, System.currentTimeMillis());
    }

    public boolean isControlledEntity(Entity entity) {
        return remainingHits.containsKey(entity.getUniqueId());
    }

    public UUID getOwner(Entity entity) {
        return ownerOf.get(entity.getUniqueId());
    }

    public void acquireTarget(LivingEntity minion, Player owner, Player target) {
        currentTarget.remove(minion.getUniqueId());

        if (target != null && target.isOnline() && !target.isDead() && target.getGameMode() != GameMode.SPECTATOR) {
            currentTarget.put(minion.getUniqueId(), target.getUniqueId());
            lastLandedHitAt.put(minion.getUniqueId(), System.currentTimeMillis());
            return;
        }

        Player nearest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player candidate : minion.getWorld().getPlayers()) {
            if (!candidate.isOnline() || candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(owner, candidate)) continue;

            double distance = minion.getLocation().distance(candidate.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                nearest = candidate;
            }
        }

        if (nearest != null) {
            currentTarget.put(minion.getUniqueId(), nearest.getUniqueId());
            lastLandedHitAt.put(minion.getUniqueId(), System.currentTimeMillis());
        }
    }

    public Player getCurrentTarget(LivingEntity minion) {
        UUID targetId = currentTarget.get(minion.getUniqueId());
        if (targetId == null) return null;
        Player target = Bukkit.getPlayer(targetId);
        return (target != null && target.isOnline() && !target.isDead() && target.getGameMode() != GameMode.SPECTATOR) ? target : null;
    }

    public boolean shouldTeleportToTarget(LivingEntity minion) {
        Long last = lastLandedHitAt.get(minion.getUniqueId());
        if (last == null) return false;
        return System.currentTimeMillis() - last >= teleportTimeoutMillis;
    }

    public void markTeleported(LivingEntity minion) {
        lastLandedHitAt.put(minion.getUniqueId(), System.currentTimeMillis());
    }

    public boolean canAttack(LivingEntity entity) {
        long last = lastAttackAt.getOrDefault(entity.getUniqueId(), 0L);
        return System.currentTimeMillis() - last >= attackCooldownMillis;
    }

    public void markAttacked(LivingEntity entity) {
        lastAttackAt.put(entity.getUniqueId(), System.currentTimeMillis());
    }

    public boolean registerHit(LivingEntity entity, Player attacker) {
        UUID id = entity.getUniqueId();
        UUID ownerId = ownerOf.get(id);
        if (ownerId == null) return false;

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null && !teamManager.isEnemy(owner, attacker)) {
            return false;
        }

        Integer remaining = remainingHits.get(id);
        if (remaining == null) return false;

        remaining--;
        if (remaining <= 0) {
            remainingHits.remove(id);
            handleDeath(entity);
            return true;
        }

        remainingHits.put(id, remaining);
        entity.getWorld().playSound(entity.getLocation(), getHurtSound(), 1f, 1f);
        entity.playHurtAnimation(1);
        return false;
    }

    protected void handleDeath(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        ownerOf.remove(id);
        currentTarget.remove(id);
        lastLandedHitAt.remove(id);
        lastAttackAt.remove(id);

        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 2);
        entity.getWorld().playSound(entity.getLocation(), getDeathSound(), 1f, 1f);
        entity.remove();
    }

    public void clearAll() {
        remainingHits.clear();
        ownerOf.clear();
        currentTarget.clear();
        lastLandedHitAt.clear();
        lastAttackAt.clear();
    }

    public void setCombatService(CombatService combatService) {
        this.combatService = combatService;
    }

    public abstract void onControlledAttack(LivingEntity attackerEntity, Player victim);
    protected abstract Sound getHurtSound();
    protected abstract Sound getDeathSound();
}