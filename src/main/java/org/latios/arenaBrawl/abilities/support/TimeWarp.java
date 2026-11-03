package org.latios.arenaBrawl.abilities.support;

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
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TimeWarp implements Ability {

    private static final int SMOKE_INTERVAL_TICKS = 2;
    private static final Map<UUID, BukkitRunnable> PENDING = new HashMap<>();

    private final Plugin plugin;
    private final long cooldownMs;
    private final long durationMs;
    private final double healAmount;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;

    public TimeWarp(Plugin plugin, CooldownManager cooldownManager, PlayerHealthManager healthManager,
                    CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.durationMs = Math.max(50L, config.getLong("duration-ms", 6000L));
        this.healAmount = config.getDouble("heal-amount", 300.0);
        this.cost = new CooldownCost(cooldownManager, "timewarp", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
    }

    @Override
    public String getName() {
        return "Time Warp";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        cancel(player.getUniqueId());

        Location anchor = player.getLocation().clone();
        long delayTicks = durationMs / 50L;

        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 0.35, 0), 20, 0.3, 0.5, 0.3, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);

        BukkitRunnable task = new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    TimeWarp.cancel(player.getUniqueId());
                    return;
                }

                if (elapsed % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double offsetX = (Math.random() - 0.5) * 3.0;
                        double offsetZ = (Math.random() - 0.5) * 3.0;
                        player.getWorld().spawnParticle(Particle.CLOUD,
                                anchor.clone().add(offsetX, 0, offsetZ),
                                1, 0, 0, 0, 0);
                    }
                }

                elapsed += SMOKE_INTERVAL_TICKS;
                if (elapsed < delayTicks) return;

                TimeWarp.cancel(player.getUniqueId());

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

                player.teleport(anchor);

                healthManager.heal(player, healAmount, getName());
            }
        };

        PENDING.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, SMOKE_INTERVAL_TICKS);
        return true;
    }

    /** Cancels a pending warp (match end, death cleanup, etc.). */
    public static void clear(Player player) {
        cancel(player.getUniqueId());
    }

    private static void cancel(UUID playerId) {
        BukkitRunnable task = PENDING.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public String getDescription() {
        return "Leave a cloud of smoke where you stand and keep fighting. After "
                + (durationMs / 1000L) + "s you snap back to that spot, restoring "
                + (int) healAmount + " health.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Return Delay", (durationMs / 1000L) + "s"),
                new AbilityStat("Healing", String.valueOf((int) healAmount)),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
