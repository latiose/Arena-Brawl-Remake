package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.latios.arenaBrawl.abilities.BaseMinionManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.UUID;

public class ZombieEntityManager extends BaseMinionManager {

    private final int zombieHitCount;
    private final double zombieDamage;

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
            registerControlledEntity(zombie, owner.getUniqueId(), zombieHitCount);
            acquireTarget(zombie, owner, target);
        }
    }

    @Override
    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        UUID ownerId = ownerOf.get(attackerEntity.getUniqueId());
        Player owner = ownerId != null ? org.bukkit.Bukkit.getPlayer(ownerId) : null;
        if (teamManager.isAlly(owner, victim)) return;

        combatService.applyMinionDamage(owner, victim, zombieDamage, "Zombie", victim.getLocation());
    }

    @Override
    protected Sound getHurtSound() { return Sound.ENTITY_ZOMBIE_HURT; }

    @Override
    protected Sound getDeathSound() { return Sound.ENTITY_ZOMBIE_DEATH; }
}