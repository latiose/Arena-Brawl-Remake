package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Instantly heals the caster, but after the premonition duration the healed
 * health is lost again. The health loss is cancelled if the caster died during
 * the window (it simply never applies).
 */
public class Premonition implements Ability {

    private static final Map<UUID, BukkitRunnable> PENDING = new HashMap<>();

    /** Cancels a pending health loss (death cleanup, match end, etc.). */
    public static void clear(Player player) {
        BukkitRunnable task = PENDING.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private final Plugin plugin;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final CooldownManager cooldownManager;
    private final long chargeTimeMillis;
    private final double healAmount;
    private final long durationMillis;

    public Premonition(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                       PlayerHealthManager healthManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.healthManager = healthManager;
        this.healAmount = config.getDouble("heal-amount", 600.0);
        this.durationMillis = config.getLong("duration-ms", 30000L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.cost = new UltimateCost(cooldownManager, usageManager, "premonition");
    }

    @Override
    public String getName() {
        return "Premonition";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "premonition", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        double healed = healthManager.heal(player, healAmount,"Premonition");

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.4f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 50, 0.4, 0.7, 0.4, 0.1);

        final double amountToLose = healed;
        final int maxSeconds = (int) (durationMillis / 1000L);

        BukkitRunnable task = new BukkitRunnable() {
            int secondsElapsed = 0;

            @Override
            public void run() {
                boolean cancelled = !player.isOnline()
                        || player.isDead()
                        || healthManager.isEliminated(player);

                if (cancelled) {
                    // If the player died during the premonition window, the health loss is cancelled.
                    PENDING.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                secondsElapsed++;

                if (secondsElapsed >= maxSeconds) {
                    PENDING.remove(player.getUniqueId());
                    if (amountToLose > 0) {
                        healthManager.damage(player, amountToLose, null); //true dmg
                        player.sendMessage(MessageUtils.negative() + String.format(
                                "§3Your premonition hurt you for §c%d§3 health!",
                                (int) Math.round(amountToLose)));
                    }
                    player.getWorld().spawnParticle(Particle.WITCH,
                            player.getLocation().add(0, 1.2, 0), 25, 0.4, 0.6, 0.4, 0.05);
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.8f);
                    cancel();
                }
            }
        };

        Premonition.clear(player);
        PENDING.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 20L, 20L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Foresaw your wounds to come: instantly heals you for " + (int) healAmount
                + ", but after " + (durationMillis / 1000) + "s the health is lost again. "
                + "If you die during the window, the loss never happens.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Instant Healing", String.valueOf((int) healAmount)),
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Cost Later", (int) healAmount + " health"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000L) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}
