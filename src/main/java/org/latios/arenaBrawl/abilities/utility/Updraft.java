package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Vertical mobility. Launches the caster upwards and then softens the fall for
 * a short window, giving reliable access to higher levels of the arena.
 */
public class Updraft implements Ability {

    private final long cooldownMs;
    private final double launchVelocity;
    private final long glideMs;
    private final double maxFallSpeed;

    private final Plugin plugin;
    private final AbilityCost cost;

    public Updraft(Plugin plugin, CooldownManager cooldownManager,
                   CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 20000L);
        this.launchVelocity = config.getDouble("launch-velocity", 1.3);
        this.glideMs = config.getLong("glide-ms", 1500L);
        this.maxFallSpeed = config.getDouble("max-fall-speed", -0.15);

        this.cost = new CooldownCost(cooldownManager, "updraft", cooldownMs, combatUpgradeManager);
    }

    @Override
    public String getName() {
        return "Updraft";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Vector velocity = player.getVelocity();
        player.setVelocity(new Vector(
                velocity.getX() * 0.6,
                launchVelocity,
                velocity.getZ() * 0.6
        ));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.8f, 1.6f);
        player.getWorld().spawnParticle(
                Particle.CLOUD, player.getLocation(), 25, 0.5, 0.2, 0.5, 0.08
        );

        int maxTicks = (int) Math.max(1L, glideMs / 50L);

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || ticks++ >= maxTicks) {
                    cancel();
                    return;
                }

                if (player.isOnGround()) {
                    cancel();
                    return;
                }

                Vector current = player.getVelocity();
                if (current.getY() < maxFallSpeed) {
                    player.setVelocity(new Vector(current.getX(), maxFallSpeed, current.getZ()));
                }

                player.getWorld().spawnParticle(
                        Particle.CLOUD, player.getLocation(), 2, 0.3, 0.1, 0.3, 0.01
                );
                player.getWorld().spawnParticle(
                        Particle.END_ROD, player.getLocation().add(0, 0.5, 0),
                        1, 0.15, 0.1, 0.15, 0.0
                );
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Launches you upwards and softens your fall for a short moment, letting you reach higher levels.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Launch", String.format("%.1f", launchVelocity)),
                new AbilityStat("Glide", (glideMs / 1000.0) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
