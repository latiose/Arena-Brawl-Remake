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
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.UUID;

public class SkeletonEntityManager extends BaseMinionManager {

    private final int skeletonHitCount;
    private final double arrowDamage;

    public SkeletonEntityManager(TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        super(teamManager, combatService, Long.MAX_VALUE,
                config.getLong("attack-cooldown-millis", 1_000L));

        this.skeletonHitCount = config.getInt("skeleton-hits-to-kill", 6);
        this.arrowDamage = config.getDouble("arrow-damage", 100.0);
    }

    public void summonSkeleton(Player owner, Player target) {
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
        });

        EntityCleanupUtils.markAsArenaEntity(skeleton);
        registerControlledEntity(skeleton, owner.getUniqueId(), skeletonHitCount);
        acquireTarget(skeleton, owner, target);
    }

    @Override
    public boolean shouldTeleportToTarget(LivingEntity minion) {
        return false;
    }

    @Override
    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        UUID ownerId = ownerOf.get(attackerEntity.getUniqueId());
        Player owner = ownerId != null ? org.bukkit.Bukkit.getPlayer(ownerId) : null;

        if (owner != null && teamManager.isAlly(owner, victim)) return;

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