package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Meditation implements Ability {

    private static final long CHECK_INTERVAL_TICKS = 2L;
    private static final int HEAL_INTERVAL_TICKS = 10;
    private static final double MOVE_THRESHOLD_SQ = 0.09;
    private static final double MAX_VERTICAL_DRIFT = 0.5;
    private static final Map<UUID, BukkitRunnable> ACTIVE = new HashMap<>();

    private final long cooldownMs;
    private final long maxDurationMs;
    private final double healPerSecond;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Meditation(CooldownManager cooldownManager, PlayerHealthManager healthManager,
                      CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 25000L);
        this.maxDurationMs = config.getLong("max-duration-ms", 6000L);
        this.healPerSecond = config.getDouble("heal-per-second", 60.0);
        this.cost = new CooldownCost(cooldownManager, "meditation", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Meditation";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        stop(player, null);

        Location anchor = player.getLocation().clone();
        int maxTicks = (int) (maxDurationMs / 50L);
        double healPerPulse = healPerSecond * (HEAL_INTERVAL_TICKS / 20.0);

        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.2f);

        BukkitRunnable task = new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    stop(player, null);
                    return;
                }

                Location now = player.getLocation();
                double dx = now.getX() - anchor.getX();
                double dz = now.getZ() - anchor.getZ();
                if (dx * dx + dz * dz > MOVE_THRESHOLD_SQ
                        || Math.abs(now.getY() - anchor.getY()) > MAX_VERTICAL_DRIFT) {
                    stop(player, MessageUtils.negative() + "§3Your meditation was broken because you moved.");
                    return;
                }

                elapsed += CHECK_INTERVAL_TICKS;
                Location center = now.clone().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.ENCHANT, center, 8, 0.5, 0.8, 0.5, 0.4);

                if (elapsed % HEAL_INTERVAL_TICKS == 0) {
                    healthManager.heal(player, healPerPulse, getName());
                    player.getWorld().spawnParticle(Particle.HEART, now.clone().add(0, 2, 0), 1, 0.3, 0.1, 0.3, 0);
                }

                if (elapsed >= maxTicks) {
                    stop(player, MessageUtils.positive() + null);
                }
            }
        };

        ACTIVE.put(player.getUniqueId(), task);
        task.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, CHECK_INTERVAL_TICKS);
        return true;
    }

    public static void interrupt(Player victim) {
        stop(victim, MessageUtils.negative() + "§3Your meditation was interrupted by damage.");
    }

    private static void stop(Player player, String message) {
        BukkitRunnable task = ACTIVE.remove(player.getUniqueId());
        if (task == null) return;

        task.cancel();
        if (message != null && player.isOnline()) {
            player.sendMessage(message);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
        }
        else if(player.isOnline()){
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
        }
    }

    @Override
    public String getDescription() {
        return "Stand still to heal " + (int) healPerSecond + " HP per second for up to "
                + (maxDurationMs / 1000L) + "s. Moving or taking damage cancels it.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Healing", (int) healPerSecond + " HP/s"),
                new AbilityStat("Max Duration", (maxDurationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}