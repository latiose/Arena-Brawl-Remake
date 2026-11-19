package org.latios.arenaBrawl.abilities.support;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Individualistic self-sustain. Regenerates health while the caster keeps moving; standing still
 * for a short moment breaks it. The inverse of Meditation, which rewards staying put.
 */
public class Momentum implements Ability {

    private static final long CHECK_INTERVAL_TICKS = 2L;
    private static final double MOVEMENT_EPSILON_SQ = 0.0004; // ~0.02 blocks

    private static final Map<UUID, BukkitRunnable> ACTIVE = new HashMap<>();

    private final long cooldownMs;
    private final long durationMs;
    private final double healPerSecond;
    private final long stationaryBreakMs;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Momentum(Plugin plugin, CooldownManager cooldownManager, PlayerHealthManager healthManager,
                    CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 28000L);
        this.durationMs = config.getLong("duration-ms", 6000L);
        this.healPerSecond = config.getDouble("heal-per-second", 35.0);
        this.stationaryBreakMs = config.getLong("stationary-break-ms", 500L);

        this.cost = new CooldownCost(cooldownManager, "momentum", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Momentum";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Regenerate health while you keep moving. Standing still for half a second ends it early.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Healing", (int) healPerSecond + " HP/s"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Requires", "Moving"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    /**
     * Cancels a running Momentum. Called when the match ends, the player dies,
     * leaves to the hub or disconnects, so the healing never bleeds into the
     * lobby or the next match.
     */
    public static void clear(Player player) {
        stop(player, null);
    }

    private static void stop(Player player, String message) {
        BukkitRunnable task = ACTIVE.remove(player.getUniqueId());
        if (task == null) return;

        task.cancel();
        if (player.isOnline()) {
            if (message != null) {
                player.sendMessage(message);
            }
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.3f);
        }
    }

    @Override
    public boolean activate(Player player) {
        if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        stop(player, null);

        long maxTicks = durationMs / 50L;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 10.0f, 1.5f);

        BukkitRunnable task = new BukkitRunnable() {
            long elapsedTicks = 0;
            Location lastPosition = player.getLocation().clone();
            long lastMoveAt = System.currentTimeMillis();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || elapsedTicks >= maxTicks) {
                    clear(player);
                    return;
                }

                Location current = player.getLocation();

                if (current.distanceSquared(lastPosition) > MOVEMENT_EPSILON_SQ) {
                    lastPosition = current.clone();
                    lastMoveAt = System.currentTimeMillis();
                }

                if (System.currentTimeMillis() - lastMoveAt >= stationaryBreakMs) {
                    stop(player, MessageUtils.negative() + "§3Your Momentum ended because you stopped moving.");
                    return;
                }

                if (elapsedTicks % 10L == 0L) {
                    healthManager.heal(player, healPerSecond * 0.5,"Momentum");
                    Location body = player.getLocation().add(0, 1.0, 0);
                    body.getWorld().spawnParticle(Particle.HEART, body, 1, 0.3, 0.3, 0.3, 0.0);
                    body.getWorld().spawnParticle(Particle.CRIT, body.clone().add(0, -0.5, 0), 2, 0.3, 0.1, 0.3, 0.0);
                }

                elapsedTicks += CHECK_INTERVAL_TICKS;
            }
        };

        ACTIVE.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, CHECK_INTERVAL_TICKS);

        return true;
    }
}
