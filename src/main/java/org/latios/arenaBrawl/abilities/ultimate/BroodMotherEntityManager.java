
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BroodMotherEntityManager {

    private static final int BOSS_HIT_COUNT = 6;
    private static final int SPIDERLING_HIT_COUNT = 3;
    private static final int SPIDERLING_COUNT = 4;
    private static final long POISON_DURATION_MILLIS = 6_000;
    private static final double SPIDERLING_DAMAGE = 5.0;
    private static final long TELEPORT_TIMEOUT_MILLIS = 7_000; // teleport if no hit landed in this window
    private static final long ATTACK_COOLDOWN_MILLIS = 1_000;

    private final Map<UUID, Integer> remainingHits = new HashMap<>();
    private final Map<UUID, UUID> ownerOf = new HashMap<>();
    private final Set<UUID> bossEntities = new HashSet<>();
    private final Map<UUID, UUID> currentTarget = new HashMap<>(); // spider -> target player
    private final Map<UUID, Long> lastLandedHitAt = new HashMap<>(); // resets the 7s teleport timer
    private final Map<UUID, Long> lastAttackAt = new HashMap<>();

    private final DebuffManager debuffManager;
    private final TeamManager teamManager;
    private CombatService combatService;

    public BroodMotherEntityManager(DebuffManager debuffManager, TeamManager teamManager, CombatService combatService) {
        this.debuffManager = debuffManager;
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    public void summonBoss(Player owner,Player target) {
        Spider spider = owner.getWorld().spawn(owner.getLocation(), Spider.class, s -> {
            s.setCustomNameVisible(false);
            s.setRemoveWhenFarAway(false);
            s.setAI(true);
            var maxHealthAttr = s.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(200);
            s.setHealth(200);
        });

        EntityCleanupUtils.markAsArenaEntity(spider);
        registerControlledEntity(spider, owner.getUniqueId(), BOSS_HIT_COUNT, true);
        acquireTarget(spider, owner,target);
    }

    private void spawnSpiderling(Location location, UUID ownerId) {
        CaveSpider spiderling = location.getWorld().spawn(location, CaveSpider.class, s -> {
            s.setCustomNameVisible(false);
            s.setRemoveWhenFarAway(false);
            s.setAI(true);

            var maxHealthAttr = s.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(50);
            s.setHealth(50);

        });

        EntityCleanupUtils.markAsArenaEntity(spiderling);
        registerControlledEntity(spiderling, ownerId, SPIDERLING_HIT_COUNT, false);

        Player owner = org.bukkit.Bukkit.getPlayer(ownerId);
        if (owner != null) acquireTarget(spiderling, owner, null);
    }

    private void registerControlledEntity(LivingEntity entity, UUID ownerId, int hitCount, boolean isBoss) {
        UUID id = entity.getUniqueId();
        remainingHits.put(id, hitCount);
        ownerOf.put(id, ownerId);
        lastLandedHitAt.put(id, System.currentTimeMillis());
        if (isBoss) bossEntities.add(id);
    }

    public boolean isControlledEntity(Entity entity) {
        return remainingHits.containsKey(entity.getUniqueId());
    }

    public UUID getOwner(Entity entity) {
        return ownerOf.get(entity.getUniqueId());
    }


    /** Picks the nearest valid enemy to the owner and locks the spider onto them. */
    public void acquireTarget(LivingEntity spider, Player owner, Player target) {
        currentTarget.remove(spider.getUniqueId());

        if (target != null && target.isOnline() && !target.isDead() && target.getGameMode() != GameMode.SPECTATOR) {
            currentTarget.put(spider.getUniqueId(), target.getUniqueId());
            lastLandedHitAt.put(spider.getUniqueId(), System.currentTimeMillis());
            return;
        }

        Player nearest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player candidate : spider.getWorld().getPlayers()) {
            if (!candidate.isOnline() || candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(owner, candidate)) continue;

            double distance = spider.getLocation().distance(candidate.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                nearest = candidate;
            }
        }

        if (nearest != null) {
            currentTarget.put(spider.getUniqueId(), nearest.getUniqueId());
            lastLandedHitAt.put(spider.getUniqueId(), System.currentTimeMillis());
        }
    }

    public Player getCurrentTarget(LivingEntity spider) {
        UUID targetId = currentTarget.get(spider.getUniqueId());
        if (targetId == null) return null;
        Player target = org.bukkit.Bukkit.getPlayer(targetId);
        return (target != null && target.isOnline() && !target.isDead()) ? target : null;
    }

    /** True if this spider hasn't landed a hit within the timeout window — caller should teleport it to its target. */
    public boolean shouldTeleportToTarget(LivingEntity spider) {
        Long last = lastLandedHitAt.get(spider.getUniqueId());
        if (last == null) return false;
        return System.currentTimeMillis() - last >= TELEPORT_TIMEOUT_MILLIS;
    }

    public void markTeleported(LivingEntity spider) {
        lastLandedHitAt.put(spider.getUniqueId(), System.currentTimeMillis()); // reset window after teleporting in
    }

    public boolean canAttack(LivingEntity entity) {
        long last = lastAttackAt.getOrDefault(entity.getUniqueId(), 0L);
        return System.currentTimeMillis() - last >= ATTACK_COOLDOWN_MILLIS;
    }

    public void markAttacked(LivingEntity entity) {
        lastAttackAt.put(entity.getUniqueId(), System.currentTimeMillis());
    }



    /**
     * Registers one melee hit FROM A PLAYER against a controlled entity.
     * Only counts if the attacker is an enemy of the spider's owner (prevents friendly kill).
     * Returns true if this hit killed it.
     */
    public boolean registerHit(LivingEntity entity, Player attacker) {
        UUID id = entity.getUniqueId();
        UUID ownerId = ownerOf.get(id);
        if (ownerId == null) return false;

        Player owner = org.bukkit.Bukkit.getPlayer(ownerId);
        if (owner != null && !teamManager.isEnemy(owner, attacker)) {
            return false; // owner or their ally cannot damage their own spider
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
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SPIDER_HURT, 1f, 1f);
        entity.playHurtAnimation(1);
        return false;
    }

    private void handleDeath(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        UUID ownerId = ownerOf.remove(id);
        currentTarget.remove(id);
        lastLandedHitAt.remove(id);
        lastAttackAt.remove(id);

        boolean wasBoss = bossEntities.remove(id);

        entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 3);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

        if (wasBoss) {
            for (int i = 0; i < SPIDERLING_COUNT; i++) {
                spawnSpiderling(entity.getLocation(), ownerId);
            }
        }

        entity.remove();
    }

    /** Called when a controlled spider lands a melee hit on its target player. */
    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        lastLandedHitAt.put(attackerEntity.getUniqueId(), System.currentTimeMillis());

        boolean isBoss = bossEntities.contains(attackerEntity.getUniqueId());
        UUID ownerId = ownerOf.get(attackerEntity.getUniqueId());
        Player owner = ownerId != null ? org.bukkit.Bukkit.getPlayer(ownerId) : null;

        if (isBoss) {
            boolean applied = debuffManager.tryApply(owner, victim, DebuffType.POISON, POISON_DURATION_MILLIS);
            if (!applied) return;
            victim.sendMessage(MessageUtils.negative() + "§3You were poisoned by a Broodmother!");
        } else {

            combatService.applyMinionDamage(owner, victim, SPIDERLING_DAMAGE, "Spiderling",victim.getLocation());
        }
    }
    public void clearAll() {
        remainingHits.clear();
        ownerOf.clear();
        bossEntities.clear();
        currentTarget.clear();
        lastLandedHitAt.clear();
        lastAttackAt.clear();
    }

    public void setCombatService(CombatService combatService) {
        this.combatService = combatService;
    }
}