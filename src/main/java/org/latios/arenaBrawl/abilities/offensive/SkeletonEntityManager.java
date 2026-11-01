package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.latios.arenaBrawl.abilities.BaseMinionManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkeletonEntityManager extends BaseMinionManager {

    private final int skeletonHitCount;
    private final double arrowDamage;
    private final int maxSkeletons = 4;

    private final Map<UUID, Integer> playerSkeletonCount = new HashMap<>();

    public SkeletonEntityManager(TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        super(teamManager, combatService, Long.MAX_VALUE,
                config.getLong("attack-cooldown-millis", 1_000L));

        this.skeletonHitCount = config.getInt("skeleton-hits-to-kill", 6);
        this.arrowDamage = config.getDouble("arrow-damage", 100.0);
    }

    public boolean summonSkeleton(Player owner, Player target) {
        UUID ownerId = owner.getUniqueId();
        int currentCount = playerSkeletonCount.getOrDefault(ownerId, 0);

        if (currentCount >= maxSkeletons) {
            return false;
        }

        Location spawnLoc = owner.getLocation();
        Skeleton skeleton = spawnLoc.getWorld().spawn(spawnLoc, Skeleton.class, s -> {
            s.setCustomNameVisible(false);
            s.setRemoveWhenFarAway(false);
            s.setAI(true);
            s.setInvulnerable(false);
            s.setCollidable(true);
            s.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
            s.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
            s.getEquipment().setItemInMainHandDropChance(0f);
            s.getEquipment().setHelmetDropChance(0f);
            s.getEquipment().setChestplateDropChance(0f);
            s.getEquipment().setLeggingsDropChance(0f);
            s.getEquipment().setBootsDropChance(0f);

            var maxHealthAttr = s.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(100);
            s.setHealth(100);

            playerSkeletonCount.put(ownerId, currentCount + 1);
        });

        EntityCleanupUtils.markAsArenaEntity(skeleton);
        registerControlledEntity(skeleton, owner, skeletonHitCount);
        acquireTarget(skeleton, owner, target);
        return true;
    }

    @Override
    public void acquireTarget(LivingEntity minion, Player owner, Player target) {
        if (target != null && target.isOnline() && !target.isDead()
                && target.getGameMode() != org.bukkit.GameMode.SPECTATOR
                && isEnemyOf(minion, target)) {

            setSkeletonTarget(minion, target);
            return;
        }

        acquireTarget(minion);
        Player reacquired = getCurrentTarget(minion);
        if (reacquired != null) {
            setSkeletonTarget(minion, reacquired);
        } else {
            if (minion instanceof Skeleton sk) {
                sk.setTarget(null);
            }
            currentTarget.remove(minion.getUniqueId());
        }
    }

    private void setSkeletonTarget(LivingEntity minion, Player enemy) {
        currentTarget.put(minion.getUniqueId(), enemy.getUniqueId());
        lastLandedHitAt.put(minion.getUniqueId(), System.currentTimeMillis());

        // Asignar el objetivo directamente a la IA del esqueleto de Bukkit
        if (minion instanceof Skeleton sk) {
            sk.setTarget(enemy);
        }
    }

    @Override
    protected void handleDeath(LivingEntity entity) {
        UUID ownerId = ownerOf.get(entity.getUniqueId());

        super.handleDeath(entity); // Elimina de colecciones base y borra entidad

        if (ownerId != null) {
            reduceSkeletonCount(ownerId);
        }
    }

    public void reduceSkeletonCount(UUID ownerId) {
        int current = playerSkeletonCount.getOrDefault(ownerId, 0);
        if (current <= 1) {
            playerSkeletonCount.remove(ownerId);
        } else {
            playerSkeletonCount.put(ownerId, current - 1);
        }
    }

    @Override
    public void clearAll() {
        super.clearAll();
        playerSkeletonCount.clear();
    }

    @Override
    public void clearForPlayer(Player player) {
        super.clearForPlayer(player);
        playerSkeletonCount.remove(player.getUniqueId());
    }

    @Override
    public void clearForMatch(Match match) {
        super.clearForMatch(match);
        for (Player player : match.getAllPlayers()) {
            playerSkeletonCount.remove(player.getUniqueId());
        }
    }

    @Override
    public boolean shouldTeleportToTarget(LivingEntity minion) {
        return false;
    }

    @Override
    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        if (!isEnemyOf(attackerEntity, victim)) return;

        UUID ownerId = ownerOf.get(attackerEntity.getUniqueId());
        Player owner = ownerId != null ? org.bukkit.Bukkit.getPlayer(ownerId) : null;
        if (owner == null) return;

        combatService.applyMinionDamage(owner, victim, arrowDamage, "Skeleton", victim.getLocation());
    }

    @Override
    protected Sound getHurtSound() { return Sound.ENTITY_SKELETON_HURT; }

    @Override
    protected Sound getDeathSound() { return Sound.ENTITY_SKELETON_DEATH; }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}