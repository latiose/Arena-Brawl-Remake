package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.latios.arenaBrawl.abilities.BaseMinionManager;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ZombieEntityManager extends BaseMinionManager {

    private final int zombieHitCount;
    private final double zombieDamage;

    /** Per-zombie damage overrides used by special zombies such as the Impregnation baby. */
    private final Map<UUID, Double> damageOverrides = new HashMap<>();

    public ZombieEntityManager(TeamManager teamManager, CombatService combatService,
                               org.latios.arenaBrawl.abilities.config.AbilityConfig config) {
        super(teamManager, combatService,
                config.getLong("teleport-timeout-millis", 10_000L),
                config.getLong("attack-cooldown-millis", 1_000L));

        this.zombieHitCount = config.getInt("zombie-hits-to-kill", 3);
        this.zombieDamage = config.getDouble("zombie-damage", 20.0);
    }

    public void summonZombies(Player owner, Player target, int count) {
        Location spawnLoc = owner.getLocation();

        for (int i = 0; i < count; i++) {
            Zombie zombie = spawnLoc.getWorld().spawn(spawnLoc, Zombie.class, z -> {
                z.setCustomNameVisible(false);
                z.setRemoveWhenFarAway(false);
                z.setAI(true);
                z.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
                z.getEquipment().setItemInMainHandDropChance(0f);
                z.getEquipment().setHelmetDropChance(0f);
                z.getEquipment().setChestplateDropChance(0f);
                z.getEquipment().setLeggingsDropChance(0f);
                z.getEquipment().setBootsDropChance(0f);
                var maxHealthAttr = z.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) maxHealthAttr.setBaseValue(100);
                z.setHealth(100);
            });

            EntityCleanupUtils.markAsArenaEntity(zombie);
            registerControlledEntity(zombie, owner, zombieHitCount);
            acquireTarget(zombie, owner, target);
        }
    }

    /**
     * Spawns a baby zombie with custom health, hit count and damage. It is
     * registered in this manager so the regular {@link ZombieAI} controls it
     * exactly like every other zombie.
     */
    public void summonBabyZombie(Player owner, Location spawnLoc, double health,
                                 int hitsToKill, double damage) {
        Zombie zombie = spawnLoc.getWorld().spawn(spawnLoc, Zombie.class, z -> {
            z.setBaby(true);
            z.setCustomNameVisible(false);
            z.setRemoveWhenFarAway(false);
            z.setAI(true);
            z.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
            z.getEquipment().setItemInMainHandDropChance(0f);
            z.getEquipment().setHelmetDropChance(0f);
            z.getEquipment().setChestplateDropChance(0f);
            z.getEquipment().setLeggingsDropChance(0f);
            z.getEquipment().setBootsDropChance(0f);
            var maxHealthAttr = z.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(health);
            z.setHealth(health);
        });

        EntityCleanupUtils.markAsArenaEntity(zombie);
        registerControlledEntity(zombie, owner, hitsToKill);
        damageOverrides.put(zombie.getUniqueId(), damage);
        acquireTarget(zombie, owner, null);
    }

    @Override
    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        if (!isEnemyOf(attackerEntity, victim)) return;

        UUID ownerId = ownerOf.get(attackerEntity.getUniqueId());
        Player owner = ownerId != null ? org.bukkit.Bukkit.getPlayer(ownerId) : null;
        if (owner == null) return;

        Double override = damageOverrides.get(attackerEntity.getUniqueId());
        double damage = override != null ? override : zombieDamage;
        String source = override != null ? "Baby Zombie" : "Zombie";

        combatService.applyMinionDamage(owner, victim, damage, source, victim.getLocation());
    }


    public void clearBabiesForMatch(Match match) {
        if (match == null) return;
        List<UUID> babyIds = new ArrayList<>();
        for(Player player : match.getAllPlayers()) {
            UUID ownerId = player.getUniqueId();
            for (UUID id : damageOverrides.keySet()) {
                if (ownerId.equals(ownerOf.get(id))) {
                    babyIds.add(id);
                }
            }
        }


        for (UUID id : babyIds) {
            LivingEntity entity = activeEntities.get(id);
            if (entity != null) {
                handleDeath(entity);
            } else {
                damageOverrides.remove(id);
            }
        }
    }

    @Override
    protected void handleDeath(LivingEntity entity) {
        damageOverrides.remove(entity.getUniqueId());
        super.handleDeath(entity);
    }

    @Override
    public void clearAll() {
        super.clearAll();
        damageOverrides.clear();
    }

    @Override
    public void clearForWorld(World world) {
        super.clearForWorld(world);
        pruneDamageOverrides();
    }

    @Override
    public void clearForMatch(org.latios.arenaBrawl.game.Match match) {
        super.clearForMatch(match);
        pruneDamageOverrides();
    }

    @Override
    public void clearForPlayer(Player player) {
        super.clearForPlayer(player);
        pruneDamageOverrides();
    }

    private void pruneDamageOverrides() {
        damageOverrides.keySet().retainAll(activeEntities.keySet());
    }

    @Override
    protected Sound getHurtSound() { return Sound.ENTITY_ZOMBIE_HURT; }

    @Override
    protected Sound getDeathSound() { return Sound.ENTITY_ZOMBIE_DEATH; }
}