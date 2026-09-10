package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZombieEntityManager {

    private static final int ZOMBIE_HIT_COUNT = 4;
    private static final double ZOMBIE_DAMAGE = 20.0;
    private static final long ATTACK_COOLDOWN_MILLIS = 1_000;

    private final Map<UUID, Integer> remainingHits = new HashMap<>();
    private final Map<UUID, UUID> ownerOf = new HashMap<>();
    private final Map<UUID, UUID> currentTarget = new HashMap<>();
    private final Map<UUID, Long> lastAttackAt = new HashMap<>();
    private static final long TELEPORT_TIMEOUT_MILLIS = 10_000;
    private final Map<UUID, Long> lastLandedHitAt = new HashMap<>();
    private final TeamManager teamManager;
    private CombatService combatService;

    public ZombieEntityManager(TeamManager teamManager, CombatService combatService) {
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    public void summonZombies(Player owner, Player target, int count) {
        Location spawnLoc = owner.getLocation();

        for (int i = 0; i < count; i++) {
            Zombie zombie = spawnLoc.getWorld().spawn(spawnLoc, Zombie.class, z -> {
                z.setCustomNameVisible(false);
                z.setRemoveWhenFarAway(false);
                z.setAI(true);
                z.setBaby(false);

                if (z.getEquipment() != null) {
                    z.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
                    z.getEquipment().setHelmetDropChance(0f);
                }

                var maxHealthAttr = z.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) maxHealthAttr.setBaseValue(100);
                z.setHealth(100);
            });

            EntityCleanupUtils.markAsArenaEntity(zombie);
            registerControlledEntity(zombie, owner.getUniqueId(), ZOMBIE_HIT_COUNT);
            acquireTarget(zombie, owner, target);
        }
    }

    private void registerControlledEntity(LivingEntity entity, UUID ownerId, int hitCount) {
        UUID id = entity.getUniqueId();
        remainingHits.put(id, hitCount);
        ownerOf.put(id, ownerId);
        lastLandedHitAt.put(id, System.currentTimeMillis());
    }

    public boolean shouldTeleportToTarget(LivingEntity zombie) {
        Long last = lastLandedHitAt.get(zombie.getUniqueId());
        if (last == null) return false;
        return System.currentTimeMillis() - last >= TELEPORT_TIMEOUT_MILLIS;
    }

    public void markTeleported(LivingEntity zombie) {
        lastLandedHitAt.put(zombie.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isControlledEntity(Entity entity) {
        return remainingHits.containsKey(entity.getUniqueId());
    }

    public UUID getOwner(Entity entity) {
        return ownerOf.get(entity.getUniqueId());
    }

    public void acquireTarget(LivingEntity zombie, Player owner, Player target) {
        currentTarget.remove(zombie.getUniqueId());

        if (target != null && target.isOnline() && !target.isDead() && target.getGameMode() != GameMode.SPECTATOR) {
            currentTarget.put(zombie.getUniqueId(), target.getUniqueId());
            return;
        }

        Player nearest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player candidate : zombie.getWorld().getPlayers()) {
            if (!candidate.isOnline() || candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(owner, candidate)) continue;

            double distance = zombie.getLocation().distance(candidate.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                nearest = candidate;
            }
        }

        if (nearest != null) {
            currentTarget.put(zombie.getUniqueId(), nearest.getUniqueId());
        }
    }

    public Player getCurrentTarget(LivingEntity zombie) {
        UUID targetId = currentTarget.get(zombie.getUniqueId());
        if (targetId == null) return null;
        Player target = org.bukkit.Bukkit.getPlayer(targetId);
        return (target != null && target.isOnline() && !target.isDead()) ? target : null;
    }

    public boolean canAttack(LivingEntity entity) {
        long last = lastAttackAt.getOrDefault(entity.getUniqueId(), 0L);
        return System.currentTimeMillis() - last >= ATTACK_COOLDOWN_MILLIS;
    }

    public void markAttacked(LivingEntity entity) {
        lastAttackAt.put(entity.getUniqueId(), System.currentTimeMillis());
    }

    public boolean registerHit(LivingEntity entity, Player attacker) {
        UUID id = entity.getUniqueId();
        UUID ownerId = ownerOf.get(id);
        if (ownerId == null) return false;

        Player owner = org.bukkit.Bukkit.getPlayer(ownerId);
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
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 1f, 1f);
        entity.playHurtAnimation(1);
        return false;
    }

    private void handleDeath(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        ownerOf.remove(id);
        currentTarget.remove(id);
        lastAttackAt.remove(id);

        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 2);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_DEATH, 1f, 1f);

        entity.remove();
    }

    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        UUID ownerId = ownerOf.get(attackerEntity.getUniqueId());
        Player owner = ownerId != null ? org.bukkit.Bukkit.getPlayer(ownerId) : null;
        if(teamManager.isAlly(owner,victim)) { return;}
        combatService.applyMinionDamage(owner, victim, ZOMBIE_DAMAGE, "Zombie", victim.getLocation());
    }

    public void clearAll() {
        remainingHits.clear();
        ownerOf.clear();
        currentTarget.clear();
        lastAttackAt.clear();
    }

    public void setCombatService(CombatService combatService) {
        this.combatService = combatService;
    }
}