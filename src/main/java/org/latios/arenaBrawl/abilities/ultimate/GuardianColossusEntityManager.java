package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.BaseMinionManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

/**
 * Tank minion manager for Guardian Colossus. The golem soaks hits through the
 * normal minion hit system and periodically taunts nearby enemies by pulling
 * them toward it and slowing them.
 */
public class GuardianColossusEntityManager extends BaseMinionManager {

    private final Plugin plugin;
    private final DebuffManager debuffManager;

    private final int hitCount;
    private final double maxHealth;
    private final double attackDamage;
    private final long durationMs;
    private final double tauntRadius;
    private final long tauntIntervalMs;
    private final long tauntSlowMs;
    private final double tauntPullStrength;

    public GuardianColossusEntityManager(TeamManager teamManager, CombatService combatService,
                                         DebuffManager debuffManager, Plugin plugin,
                                         AbilityConfig config) {
        super(teamManager, combatService,
                config.getLong("teleport-timeout-millis", 60000L),
                config.getLong("attack-cooldown-millis", 1000L));

        this.plugin = plugin;
        this.debuffManager = debuffManager;
        this.hitCount = config.getInt("hits-to-kill", 30);
        this.maxHealth = config.getDouble("max-health", 3000.0);
        this.attackDamage = config.getDouble("attack-damage", 60.0);
        this.durationMs = config.getLong("duration-ms", 10000L);
        this.tauntRadius = config.getDouble("taunt-radius", 8.0);
        this.tauntIntervalMs = config.getLong("taunt-interval-ms", 2000L);
        this.tauntSlowMs = config.getLong("taunt-slow-ms", 800L);
        this.tauntPullStrength = config.getDouble("taunt-pull-strength", 0.25);
    }

    public void summonColossus(Player owner) {
        Location spawn = findSpawn(owner);
        IronGolem golem = owner.getWorld().spawn(spawn, IronGolem.class, entity -> {
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);
            entity.setPlayerCreated(true);
            entity.setCanPickupItems(false);
            entity.setAI(true);

            var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(500);
            entity.setHealth(500);
        });

        EntityCleanupUtils.markAsArenaEntity(golem);
        registerControlledEntity(golem, owner.getUniqueId(), hitCount);
        acquireTarget(golem, owner, null);

        owner.getWorld().spawnParticle(
                Particle.EXPLOSION, spawn, 2, 0.4, 0.4, 0.4, 0.0
        );
        owner.getWorld().spawnParticle(
                Particle.BLOCK, spawn, 35, 0.8, 0.8, 0.8, 0.15,
                org.bukkit.Material.IRON_BLOCK.createBlockData()
        );
        owner.getWorld().playSound(spawn, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.7f);
        owner.getWorld().playSound(spawn, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.8f);

        long expiresAt = System.currentTimeMillis() + durationMs;
        new BukkitRunnable() {
            long nextTauntAt = System.currentTimeMillis();

            @Override
            public void run() {
                if (!owner.isOnline()
                        || owner.getGameMode() == GameMode.SPECTATOR
                        || System.currentTimeMillis() >= expiresAt
                        || !golem.isValid()
                        || !isControlledEntity(golem)) {
                    if (golem.isValid()) {
                        handleDeath(golem);
                    }
                    cancel();
                    return;
                }

                drawAura(golem);

                if (System.currentTimeMillis() >= nextTauntAt) {
                    taunt(golem, owner);
                    nextTauntAt = System.currentTimeMillis() + tauntIntervalMs;
                }
            }
        }.runTaskTimer(plugin, 20L, 10L);
    }

    private void taunt(IronGolem golem, Player owner) {
        golem.getWorld().playSound(golem.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.8f);
        golem.getWorld().spawnParticle(
                Particle.ENCHANTED_HIT, golem.getLocation().add(0, 1.5, 0), 25,
                1.0, 1.0, 1.0, 0.12
        );

        for (Player enemy : golem.getWorld().getPlayers()) {
            if (enemy.isDead()
                    || enemy.getGameMode() == GameMode.SPECTATOR
                    || !teamManager.isEnemy(owner, enemy)) {
                continue;
            }

            if (enemy.getLocation().distanceSquared(golem.getLocation()) > tauntRadius * tauntRadius) {
                continue;
            }

            if (isControlledEntity(golem)) {
                registerHit(golem, enemy);
            }
            if (!isControlledEntity(golem)) {
                break;
            }

            debuffManager.tryApply(owner, enemy, DebuffType.SLOW, tauntSlowMs, 0.0);

            Vector pull = golem.getLocation().toVector().subtract(enemy.getLocation().toVector());
            if (pull.lengthSquared() > 0.01) {
                pull.normalize().multiply(tauntPullStrength);
                pull.setY(0.12);
                enemy.setVelocity(pull);
            }

            enemy.getWorld().spawnParticle(
                    Particle.CRIT, enemy.getLocation().add(0, 1.0, 0), 8,
                    0.25, 0.35, 0.25, 0.03
            );
            enemy.playSound(enemy.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.4f);
        }
    }

    private void drawAura(IronGolem golem) {
        golem.getWorld().spawnParticle(
                Particle.ENCHANT, golem.getLocation().add(0, 1.2, 0), 8,
                0.8, 0.7, 0.8, 0.02
        );
    }

    private Location findSpawn(Player owner) {
        Location base = owner.getLocation().clone();
        Vector forward = base.getDirection().setY(0);
        if (forward.lengthSquared() > 0.01) {
            forward.normalize().multiply(2.0);
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
        Player owner = ownerOf.get(attackerEntity.getUniqueId()) != null
                ? org.bukkit.Bukkit.getPlayer(ownerOf.get(attackerEntity.getUniqueId()))
                : null;

        if (owner == null || !owner.isOnline()
                || owner.getGameMode() == GameMode.SPECTATOR
                || teamManager.isAlly(owner, victim)) {
            return;
        }
        if (combatService == null) return;

        combatService.applyMinionDamage(
                owner, victim, attackDamage, "Guardian Colossus",
                victim.getLocation().add(0, victim.getHeight() * 0.5, 0)
        );

        Vector knockback = victim.getLocation().toVector()
                .subtract(attackerEntity.getLocation().toVector());
        if (knockback.lengthSquared() > 0.01) {
            knockback.normalize().multiply(0.55);
            knockback.setY(0.25);
            victim.setVelocity(victim.getVelocity().add(knockback));
        }
    }

    @Override
    protected Sound getHurtSound() {
        return Sound.ENTITY_IRON_GOLEM_HURT;
    }

    @Override
    protected Sound getDeathSound() {
        return Sound.ENTITY_IRON_GOLEM_DEATH;
    }
}
