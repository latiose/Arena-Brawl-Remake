package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.BaseMinionManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class KingsGuardEntityManager extends BaseMinionManager {

    private static final class ChargeOrder {
        private final UUID targetId;
        private final double damage;
        private final long expiresAt;

        private ChargeOrder(UUID targetId, double damage, long expiresAt) {
            this.targetId = targetId;
            this.damage = damage;
            this.expiresAt = expiresAt;
        }
    }

    private final Plugin plugin;
    private final int maxGuards;
    private final int hitsToDestroy;
    private final double normalAttackDamage;
    private final double firstChargeDamage;
    private final double repeatChargeDamage;
    private final double normalSpeedMultiplier;
    private final double chargeSpeedMultiplier;
    private final int chargeSpeedAmplifier;
    private final long chargeDurationMs;
    private final double spawnOffset;

    private final Map<UUID, ChargeOrder> charges = new HashMap<>();
    private final Set<UUID> hasCharged = new HashSet<>();

    public KingsGuardEntityManager(Plugin plugin, TeamManager teamManager, CombatService combatService,
                                   AbilityConfig config) {
        super(teamManager, combatService,
                config.getLong("teleport-timeout-millis", 60_000L),
                config.getLong("attack-cooldown-millis", 1_000L));

        this.plugin = plugin;
        this.maxGuards = config.getInt("max-guards", 3);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 3);
        this.normalAttackDamage = config.getDouble("normal-attack-damage", 50.0);
        this.firstChargeDamage = config.getDouble("first-charge-damage", 100.0);
        this.repeatChargeDamage = config.getDouble("repeat-charge-damage", 50.0);
        this.normalSpeedMultiplier = config.getDouble("normal-speed-multiplier", 1.0);
        this.chargeSpeedMultiplier = config.getDouble("charge-speed-multiplier", 2.5);
        this.chargeSpeedAmplifier = config.getInt("charge-speed-amplifier", 1);
        this.chargeDurationMs = config.getLong("charge-duration-ms", 1_000L);
        this.spawnOffset = config.getDouble("spawn-offset", 1.5);

        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public int getMaxGuards() {
        return maxGuards;
    }

    public int getGuardCount(Player owner) {
        UUID ownerId = owner.getUniqueId();
        int count = 0;

        for (Map.Entry<UUID, LivingEntity> entry : activeEntities.entrySet()) {
            if (!ownerId.equals(ownerOf.get(entry.getKey()))) continue;
            LivingEntity entity = entry.getValue();
            if (entity != null && entity.isValid()) {
                count++;
            }
        }

        return count;
    }

    public boolean summonGuard(Player owner) {
        if (getGuardCount(owner) >= maxGuards) {
            return false;
        }

        Location spawn = findSpawn(owner);
        Zombie guard = owner.getWorld().spawn(spawn, Zombie.class, zombie -> {
            zombie.setAI(true);
            zombie.setGravity(true);
            zombie.setAdult();
            zombie.setRemoveWhenFarAway(false);
            zombie.setPersistent(true);
            zombie.setShouldBurnInDay(false);
            zombie.setCanPickupItems(false);
            zombie.setCustomNameVisible(false);

            var equipment = zombie.getEquipment();
            if (equipment != null) {
                equipment.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                ItemMeta meta = equipment.getHelmet().getItemMeta();
                meta.addEnchant(Enchantment.UNBREAKING, 10, true);
                equipment.setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
                equipment.setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
                equipment.setBoots(new ItemStack(Material.GOLDEN_BOOTS));
                equipment.setItemInMainHand(new ItemStack(Material.TRIDENT));
                equipment.setHelmetDropChance(0f);
                equipment.setChestplateDropChance(0f);
                equipment.setLeggingsDropChance(0f);
                equipment.setBootsDropChance(0f);
                equipment.setItemInMainHandDropChance(0f);
            }

            var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(20);
            }
            zombie.setHealth(20);
        });

        EntityCleanupUtils.markAsArenaEntity(guard);
        registerControlledEntity(guard, owner, hitsToDestroy);
        updateTargetToNearest(guard);

        Location effectLocation = spawn.clone().add(0, 1.0, 0);
        guard.getWorld().spawnParticle(Particle.WAX_ON, effectLocation, 25, 0.35, 0.8, 0.35, 0.02);
        guard.getWorld().spawnParticle(Particle.END_ROD, effectLocation, 15, 0.25, 0.7, 0.25, 0.01);
        guard.getWorld().playSound(spawn, Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.6f);
        guard.getWorld().playSound(spawn, Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.8f);

        return true;
    }

    public int orderCharge(Player owner, Player target) {
        UUID ownerId = owner.getUniqueId();
        UUID targetId = target.getUniqueId();
        long expiresAt = System.currentTimeMillis() + chargeDurationMs;
        int ordered = 0;

        for (Map.Entry<UUID, LivingEntity> entry : new ArrayList<>(activeEntities.entrySet())) {
            LivingEntity guard = entry.getValue();
            if (guard == null || !guard.isValid()) continue;
            if (!ownerId.equals(ownerOf.get(entry.getKey()))) continue;
            if (!guard.getWorld().equals(target.getWorld())) continue;

            UUID id = entry.getKey();
            double damage = hasCharged.contains(id) ? repeatChargeDamage : firstChargeDamage;
            hasCharged.add(id);

            currentTarget.put(id, targetId);
            charges.put(id, new ChargeOrder(targetId, damage, expiresAt));

            guard.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    Math.max(1, (int) (chargeDurationMs / 50)),
                    chargeSpeedAmplifier,
                    true,
                    false
            ));

            guard.getWorld().playSound(guard.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 1.2f);
            ordered++;
        }

        return ordered;
    }

    private void tick() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, ChargeOrder>> chargeIterator = charges.entrySet().iterator();
        while (chargeIterator.hasNext()) {
            Map.Entry<UUID, ChargeOrder> entry = chargeIterator.next();
            LivingEntity guard = activeEntities.get(entry.getKey());

            if (guard == null || !guard.isValid() || now >= entry.getValue().expiresAt) {
                chargeIterator.remove();
            }
        }

        for (Map.Entry<UUID, LivingEntity> entry : new ArrayList<>(activeEntities.entrySet())) {
            UUID id = entry.getKey();
            LivingEntity guard = entry.getValue();
            if (guard == null || !guard.isValid()) continue;
            if (!(guard instanceof Mob mob)) continue;

            boolean charging = charges.containsKey(id);
            Player target = null;

            if (charging) {
                target = getCurrentTarget(guard);

                if (target == null) {
                    charges.remove(id);
                    charging = false;
                }
            }

            if (!charging) {
                target = updateTargetToNearest(guard);
            }

            if (target == null) {
                mob.setTarget(null);
                mob.getPathfinder().stopPathfinding();
                continue;
            }

            double speed = charging ? chargeSpeedMultiplier : normalSpeedMultiplier;

            mob.setTarget(target);
            mob.getPathfinder().moveTo(target.getLocation(), speed);

            if (charging) {
                guard.getWorld().spawnParticle(Particle.CRIT, guard.getLocation().add(0, 1.0, 0),
                        3, 0.2, 0.25, 0.2, 0.01);
            }
        }
    }

    private Player updateTargetToNearest(LivingEntity guard) {
        UUID guardId = guard.getUniqueId();
        Player nearest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player candidate : guard.getWorld().getPlayers()) {
            if (!candidate.isOnline() || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!isEnemyOf(guard, candidate)) continue;

            double distance = guard.getLocation().distanceSquared(candidate.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                nearest = candidate;
            }
        }

        if (nearest != null) {
            currentTarget.put(guardId, nearest.getUniqueId());
        } else {
            currentTarget.remove(guardId);
        }

        return nearest;
    }

    private Location findSpawn(Player owner) {
        Location base = owner.getLocation().clone();
        Vector forward = base.getDirection().setY(0);

        if (forward.lengthSquared() > 0.01) {
            forward.normalize().multiply(spawnOffset);
            Location candidate = base.clone().add(forward);
            if (isSpawnSafe(candidate)) {
                return candidate;
            }
        }

        return isSpawnSafe(base) ? base : owner.getLocation();
    }

    private boolean isSpawnSafe(Location location) {
        if (!location.getBlock().isPassable()
                || !location.clone().add(0, 1, 0).getBlock().isPassable()) {
            return false;
        }

        for (int down = 1; down <= 3; down++) {
            if (location.clone().subtract(0, down, 0).getBlock().getType().isSolid()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onControlledAttack(LivingEntity attackerEntity, Player victim) {
        if (!isEnemyOf(attackerEntity, victim)) return;

        UUID ownerId = ownerOf.get(attackerEntity.getUniqueId());
        Player owner = ownerId != null ? Bukkit.getPlayer(ownerId) : null;
        if (owner == null || combatService == null) return;

        UUID guardId = attackerEntity.getUniqueId();
        ChargeOrder order = charges.get(guardId);

        double damage;
        if (order != null && System.currentTimeMillis() < order.expiresAt) {
            damage = order.damage;
        } else {
            if (order != null) charges.remove(guardId);
            damage = normalAttackDamage;
        }

        combatService.applyMinionDamage(owner, victim, damage, "King's Guard",
                victim.getLocation().add(0, victim.getHeight() * 0.5, 0));

        victim.getWorld().spawnParticle(Particle.ENCHANTED_HIT, victim.getLocation().add(0, 1.0, 0),
                12, 0.3, 0.45, 0.3, 0.03);
        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
    }

    @Override
    protected void handleDeath(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        charges.remove(id);
        hasCharged.remove(id);
        super.handleDeath(entity);
    }

    @Override
    public void clearAll() {
        super.clearAll();
        charges.clear();
        hasCharged.clear();
    }

    @Override
    public void clearForWorld(World world) {
        super.clearForWorld(world);
        pruneCustomState();
    }

    @Override
    public void clearForMatch(Match match) {
        super.clearForMatch(match);
        pruneCustomState();
    }

    @Override
    public void clearForPlayer(Player player) {
        super.clearForPlayer(player);
        pruneCustomState();
    }

    private void pruneCustomState() {
        Set<UUID> activeIds = activeEntities.keySet();
        charges.keySet().retainAll(activeIds);
        hasCharged.retainAll(activeIds);
    }

    @Override
    protected Sound getHurtSound() {
        return Sound.ENTITY_ZOMBIE_HURT;
    }

    @Override
    protected Sound getDeathSound() {
        return Sound.ENTITY_ZOMBIE_DEATH;
    }
}
