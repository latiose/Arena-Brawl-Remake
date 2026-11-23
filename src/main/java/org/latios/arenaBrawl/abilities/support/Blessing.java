package org.latios.arenaBrawl.abilities.support;

import org.bukkit.GameMode;
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

import java.util.List;

public class Blessing implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final double instantHeal;
    private final double delayedHeal;
    private final double damagedHeal;
    private final long delayMs;

    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Blessing(Plugin plugin, CooldownManager cooldownManager, PlayerHealthManager healthManager,
                    CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.instantHeal = config.getDouble("instant-heal", 120.0);
        this.delayedHeal = config.getDouble("delayed-heal", 380.0);
        this.damagedHeal = config.getDouble("damaged-heal", 60.0);
        this.delayMs = config.getLong("delay-ms", 4000L);

        this.cost = new CooldownCost(cooldownManager, "blessing", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Blessing";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        healthManager.heal(player, instantHeal, getName());

        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.0, 0),
                10, 0.5, 0.5, 0.5, 0.02);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.0, 0),
                18, 0.4, 0.6, 0.4, 0.03);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.3f);

        long endAt = System.currentTimeMillis() + delayMs;

        new BukkitRunnable() {
            double lastHealth = healthManager.getHealth(player);
            boolean damaged = false;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)
                        || healthManager.isEliminated(player)) {
                    cancel();
                    return;
                }

                double current = healthManager.getHealth(player);
                if (current < lastHealth - 0.5 && !damaged) {
                    damaged = true;
                }
                lastHealth = Math.max(lastHealth, current);

                if (System.currentTimeMillis() < endAt) return;

                cancel();

                double amount = damaged ? damagedHeal : delayedHeal;
                healthManager.heal(player, amount, getName());

                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        player.getLocation().add(0, 1.0, 0), 30, 0.5, 0.7, 0.5, 0.15);
                player.getWorld().spawnParticle(Particle.HEART,
                        player.getLocation().add(0, 1.0, 0), 12, 0.4, 0.5, 0.4, 0.02);

                if (damaged) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);
                } else {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.4f);
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Blesses only yourself with a small heal, then grants a much larger heal after a short delay if you avoid taking damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Instant Heal", String.valueOf((int) instantHeal)),
                new AbilityStat("Delayed Heal", String.valueOf((int) delayedHeal)),
                new AbilityStat("Interrupted Heal", String.valueOf((int) damagedHeal)),
                new AbilityStat("Delay", (delayMs / 1000.0) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
