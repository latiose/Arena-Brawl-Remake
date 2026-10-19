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
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Individualistic self-sustain. Regenerates health while the caster keeps moving; standing still
 * for a short moment breaks it. The inverse of Meditation, which rewards staying put.
 */
public class Momentum implements Ability {

    private static final long CHECK_INTERVAL_TICKS = 2L;
    private static final double MOVEMENT_EPSILON_SQ = 0.0004; // ~0.02 blocks

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

    @Override
    public boolean activate(Player player) {
        long maxTicks = durationMs / 50L;

        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        new BukkitRunnable() {
            long elapsedTicks = 0;
            Location lastPosition = player.getLocation().clone();
            long lastMoveAt = System.currentTimeMillis();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || elapsedTicks >= maxTicks) {
                    cancel();
                    return;
                }

                Location current = player.getLocation();

                if (current.distanceSquared(lastPosition) > MOVEMENT_EPSILON_SQ) {
                    lastPosition = current.clone();
                    lastMoveAt = System.currentTimeMillis();
                }

                if (System.currentTimeMillis() - lastMoveAt >= stationaryBreakMs) {
                    player.sendMessage(MessageUtils.negative() + "§3Your Momentum ended because you stopped moving.");
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.3f);
                    cancel();
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
        }.runTaskTimer(plugin, 0L, CHECK_INTERVAL_TICKS);

        return true;
    }
}
