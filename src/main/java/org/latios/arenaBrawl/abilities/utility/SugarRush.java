package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SugarRush implements Ability {

    /** Identifies the speed buff so only Sugar Rush's own boost is removed. */
    private static final String SOURCE = "sugarrush";

    /**
     * The crash (delayed slow) is scheduled outside the ability instance, so it
     * must be tracked globally to be cancellable on death / match end.
     */
    private record ActiveSugarRush(Object token, BukkitTask task, SpeedBuffManager speed) {
    }

    private static final Map<UUID, ActiveSugarRush> ACTIVE = new ConcurrentHashMap<>();

    /**
     * Cancels the pending crash and the speed boost. Called on death, match end,
     * /hub and disconnect so the slow never lands in the lobby or next match.
     */
    public static void clear(Player player) {
        ActiveSugarRush active = ACTIVE.remove(player.getUniqueId());
        if (active == null) return;

        active.task().cancel();
        active.speed().removeBuff(player, SOURCE);
    }

    private final long cooldownMillis;
    private final int speedAmplifier;
    private final int speedDurationTicks;
    private final long slowDurationMs;
    private final SpeedBuffManager speedBuffManager;
    private final AbilityCost cost;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public SugarRush(CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                     DebuffManager debuffManager, AbilityConfig config, SpeedBuffManager speedBuffManager) {
        this.cooldownMillis = config.getLong("cooldown-ms", 30_000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 2);
        this.speedDurationTicks = config.getInt("speed-duration-ticks", 80);
        this.slowDurationMs = config.getLong("slow-duration-ms", 3_000L);

        this.cost = new CooldownCost(cooldownManager, "sugarrush", cooldownMillis, combatUpgradeManager);
        this.speedBuffManager = speedBuffManager;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Sugar rush"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        clear(player);

        speedBuffManager.applyBuff(player, speedAmplifier, speedDurationTicks * 50L, SOURCE);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 10.0f, 1f);

        UUID playerId = player.getUniqueId();
        Object token = new Object();

        BukkitTask crash = new BukkitRunnable() {
            @Override
            public void run() {
                ActiveSugarRush active = ACTIVE.get(playerId);
                if (active == null || active.token() != token) {
                    cancel();
                    return;
                }
                ACTIVE.remove(playerId);

                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR) {
                    speedBuffManager.removeBuff(player, SOURCE);
                    return;
                }

                debuffManager.tryApply(player, DebuffType.SLOW, slowDurationMs);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.02);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 10.0f, 0.8f);
            }
        }.runTaskLater(ArenaBrawlPlugin.getInstance(), speedDurationTicks);

        ACTIVE.put(playerId, new ActiveSugarRush(token, crash, speedBuffManager));

        return true;
    }

    @Override
    public String getDescription() {
        return "Gives the user speed, then crashes slowing them down";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMillis / 1000L) + "s"),
                new AbilityStat("Speed duration", (speedDurationTicks / 20) + "s"),
                new AbilityStat("Speed Level", String.valueOf(speedAmplifier + 1)),
                new AbilityStat("Slow duration", (slowDurationMs / 1000L) + "s")
        );
    }
}
