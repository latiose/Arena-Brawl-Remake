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

/**
 * Soulbound Healing halves the caster's current health for a few seconds and
 * then triples whatever health is left when the bond matures.
 */
public class SoulboundHealing implements Ability {

    private static final Map<UUID, BukkitRunnable> PENDING = new HashMap<>();

    private final Plugin plugin;
    private final long cooldownMs;
    private final long durationMs;
    private final double divideBy;
    private final double multiplyBy;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public SoulboundHealing(Plugin plugin, CooldownManager cooldownManager,
                            PlayerHealthManager healthManager, CombatUpgradeManager combatUpgradeManager,
                            AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30_000L);
        this.durationMs = Math.max(50L, config.getLong("duration-ms", 5_000L));
        this.divideBy = Math.max(1.0, config.getDouble("divide-by", 2.0));
        this.multiplyBy = Math.max(1.0, config.getDouble("multiply-by", 3.0));
        this.cost = new CooldownCost(cooldownManager, "soulboundhealing", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Soulbound Healing";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        cancel(player.getUniqueId());

        double current = healthManager.getHealth(player);
        if (current <= 1.0) return false;

        double reduced = current / divideBy;
        healthManager.damage(player, current - reduced, player);

        Location body = player.getLocation().add(0, 1.0, 0);
        body.getWorld().spawnParticle(Particle.SOUL, body, 24, 0.45, 0.7, 0.45, 0.02);
        body.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, body, 12, 0.35, 0.55, 0.35, 0.01);
        body.getWorld().playSound(body, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.7f);
        body.getWorld().playSound(body, Sound.ENTITY_WITCH_DRINK, 0.8f, 0.8f);

        long delayTicks = durationMs / 50L;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                PENDING.remove(player.getUniqueId());

                if (!player.isOnline() || player.isDead()
                        || healthManager.isEliminated(player)
                        || !config.getMatchManager().isInMatch(player)) {
                    return;
                }

                double present = healthManager.getHealth(player);
                double target = Math.min(healthManager.getMaxHealth(player), present * multiplyBy);
                double heal = target - present;

                Location body = player.getLocation().add(0, 1.0, 0);
                body.getWorld().spawnParticle(Particle.HEART, body, 18, 0.45, 0.6, 0.45, 0.0);
                body.getWorld().spawnParticle(Particle.SOUL, body, 20, 0.4, 0.6, 0.4, 0.03);
                body.getWorld().playSound(body, Sound.ITEM_TOTEM_USE, 0.7f, 1.4f);

                if (heal > 0.0) {
                    healthManager.heal(player, heal, getName());
                }
            }
        };

        PENDING.put(player.getUniqueId(), task);
        task.runTaskLater(plugin, delayTicks);
        return true;
    }

    /** Cancels a pending payout (match end, death cleanup, etc.). */
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
        return "Binds your soul for " + (durationMs / 1000L)
                + "s, halving your current health. When the bond matures, your remaining health is tripled.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Health on cast", "÷" + (int) divideBy),
                new AbilityStat("Health on payout", "x" + (int) multiplyBy),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
