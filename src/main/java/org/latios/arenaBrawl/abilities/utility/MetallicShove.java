package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MetallicShove implements Ability {

    private final long cooldownMs;
    private final double maxDistance;
    private final double speedStep;
    private final double hitRadius;
    private final long immobilizeDurationMs;
    private final long speedPerHitMs;
    private final int speedAmplifier;
    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final SpeedBuffManager speedBuffManager;
    private final AbilityConfig config;

    public MetallicShove(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                         DebuffManager debuffManager, SpeedBuffManager speedBuffManager,
                         CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 40000L);
        this.maxDistance = config.getDouble("max-distance", 25.0);
        this.speedStep = Math.max(0.25, config.getDouble("speed-step", 1.0));
        this.hitRadius = config.getDouble("hit-radius", 1.5);
        this.immobilizeDurationMs = config.getLong("immobilize-duration-ms", 3000L);
        this.speedPerHitMs = config.getLong("speed-per-hit-ms", 2000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);

        this.cost = new CooldownCost(cooldownManager, "metallicshove", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        this.speedBuffManager = speedBuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Metallic Shove";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Shoves an iron block up to " + (int) maxDistance
                + " blocks forward, immobilizing every enemy it hits for "
                + (immobilizeDurationMs / 1000.0) + "s and granting you Speed for "
                + (speedPerHitMs / 1000.0) + "s per enemy hit.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", (int) maxDistance + "m"),
                new AbilityStat("Immobilize", (immobilizeDurationMs / 1000.0) + "s"),
                new AbilityStat("Speed per hit", (speedPerHitMs / 1000.0) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location start = player.getEyeLocation().clone();
        Vector direction = start.getDirection();
        if (direction.lengthSquared() < 0.0001) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();
        Vector shootDirection = direction;

        double travelDistance = resolveTravelDistance(start, shootDirection);

        BlockData blockData = Material.IRON_BLOCK.createBlockData();
        Location spawnLocation = start.clone().add(shootDirection.clone().multiply(0.8));
        BlockDisplay display = player.getWorld().spawn(spawnLocation.clone().add(-0.5, -0.5, -0.5),
                BlockDisplay.class, d -> {
                    d.setBlock(blockData);
                    d.setGravity(false);
                    d.setPersistent(false);
                    d.setInvulnerable(true);
                });

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 10.0f, 0.7f);

        Set<UUID> hitPlayers = new HashSet<>();

        new BukkitRunnable() {
            double travelled = 0.8;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                if (travelled > travelDistance) {
                    display.remove();
                    cancel();
                    return;
                }

                Location center = start.clone().add(shootDirection.clone().multiply(travelled));
                display.teleport(center.clone().add(-0.5, -0.5, -0.5));

                center.getWorld().spawnParticle(Particle.BLOCK, center, 18, 0.35, 0.35, 0.35, 0.02, blockData);
                center.getWorld().spawnParticle(Particle.CRIT, center, 4, 0.2, 0.2, 0.2, 0.01);

                for (Entity nearby : center.getWorld().getNearbyEntities(center, hitRadius, hitRadius, hitRadius)) {
                    if (!(nearby instanceof Player target)) continue;
                    if (target.equals(player) || target.isDead()) continue;
                    if (target.getGameMode() == GameMode.SPECTATOR) continue;
                    if (!teamManager.isEnemy(player, target)) continue;
                    if (!hitPlayers.add(target.getUniqueId())) continue;

                    boolean immobilized = debuffManager.tryApply(player, target, DebuffType.IMMOBILIZE, immobilizeDurationMs);

                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 8.0f, 1.4f);
                    target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1.0, 0),
                            20, 0.3, 0.5, 0.3, 0.03, blockData);

                    speedBuffManager.applyBuff(player, speedAmplifier, speedPerHitMs * hitPlayers.size(),
                            "metallic_shove");

                }

                travelled += speedStep;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private double resolveTravelDistance(Location start, Vector direction) {
        RayTraceResult result = start.getWorld().rayTraceBlocks(start, direction, maxDistance);
        if (result != null && result.getHitPosition() != null) {
            double hitDistance = result.getHitPosition().distance(start.toVector());
            if (hitDistance > 0) {
                return Math.min(maxDistance, Math.max(speedStep, hitDistance - speedStep));
            }
        }
        return maxDistance;
    }
}
