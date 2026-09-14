package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.latios.arenaBrawl.abilities.BaseMinionManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BroodMotherEntityManager extends BaseMinionManager {

    private final int bossHitCount;
    private final int spiderlingHitCount;
    private final int spiderlingCount;
    private final long poisonDurationMillis;
    private final double spiderlingDamage;

    private final Set<UUID> bossEntities = new HashSet<>();
    private final DebuffManager debuffManager;

    public BroodMotherEntityManager(DebuffManager debuffManager, TeamManager teamManager,
                                    CombatService combatService, org.latios.arenaBrawl.abilities.config.AbilityConfig config) {
        super(teamManager, combatService,
                config.getLong("teleport-timeout-millis", 7_000L),
                config.getLong("attack-cooldown-millis", 1_000L));
        this.debuffManager = debuffManager;

        this.bossHitCount = config.getInt("brood-hp", 7);
        this.spiderlingCount = config.getInt("spiderling-count", 4);
        this.spiderlingHitCount = config.getInt("spiderling-hits-to-kill", 3);
        this.spiderlingDamage = config.getDouble("spiderling-damage", 5.0);
        this.poisonDurationMillis = config.getInt("poison-duration-sec", 6) * 1000L;
    }

    public void summonBoss(Player owner, Player target) {
        Spider spider = owner.getWorld().spawn(owner.getLocation(), Spider.class, s -> {
            s.setCustomNameVisible(false);
            s.setRemoveWhenFarAway(false);
            s.setAI(true);
            var maxHealthAttr = s.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(200);
            s.setHealth(200);
        });

        EntityCleanupUtils.markAsArenaEntity(spider);
        registerControlledEntity(spider, owner.getUniqueId(), bossHitCount);
        bossEntities.add(spider.getUniqueId());
        acquireTarget(spider, owner, target);
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
        registerControlledEntity(spiderling, ownerId, spiderlingHitCount);

        Player owner = org.bukkit.Bukkit.getPlayer(ownerId);
        if (owner != null) acquireTarget(spiderling, owner, null);
    }

    @Override
    protected void handleDeath(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        boolean wasBoss = bossEntities.remove(id);
        UUID ownerId = ownerOf.get(id);

        super.handleDeath(entity);

        if (wasBoss && ownerId != null) {
            for (int i = 0; i < spiderlingCount; i++) {
                spawnSpiderling(entity.getLocation(), ownerId);
            }
        }
    }

    @Override
    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        lastLandedHitAt.put(attackerEntity.getUniqueId(), System.currentTimeMillis());

        boolean isBoss = bossEntities.contains(attackerEntity.getUniqueId());
        UUID ownerId = ownerOf.get(attackerEntity.getUniqueId());
        Player owner = ownerId != null ? org.bukkit.Bukkit.getPlayer(ownerId) : null;
        if (teamManager.isAlly(owner, victim)) return;

        if (isBoss) {
            boolean applied = debuffManager.tryApply(owner, victim, DebuffType.POISON, poisonDurationMillis);
            if (!applied) return;
        } else {
            combatService.applyMinionDamage(owner, victim, spiderlingDamage, "Spiderling", victim.getLocation());
        }
    }

    @Override
    public void clearAll() {
        super.clearAll();
        bossEntities.clear();
    }

    @Override
    protected Sound getHurtSound() { return Sound.ENTITY_SPIDER_HURT; }

    @Override
    protected Sound getDeathSound() { return Sound.ENTITY_GENERIC_EXPLODE; }
}