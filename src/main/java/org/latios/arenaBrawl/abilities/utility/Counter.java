package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
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
import org.latios.arenaBrawl.abilities.ReflectStance;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Counter implements Ability, ReflectStance {

    private final long cooldownMs;
    private final long windowMs;
    private final double reflectMultiplier;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final AbilityConfig config;

    public Counter(Plugin plugin, CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                   AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 22000L);
        this.windowMs = config.getLong("window-ms", 1500L);
        this.reflectMultiplier = config.getDouble("reflect-multiplier", 0.75);

        this.cost = new CooldownCost(cooldownManager, "counter", cooldownMs, combatUpgradeManager);
        this.config = config;
    }

    @Override
    public String getName() {
        return "Counter";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Raises a parry stance. Every hit taken within the window is negated and its damage "
                + "is reflected back at the attacker.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Window", (windowMs / 1000.0) + "s"),
                new AbilityStat("Reflect", (int) (reflectMultiplier * 100) + "% of every hit"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        ReflectStance.ReflectState state = new ReflectStance.ReflectState(
                System.currentTimeMillis() + windowMs, reflectMultiplier, 1.0, getName());
        ReflectStance.apply(player, state);

        Location loc = player.getLocation();
        var world = loc.getWorld();
        Location body = loc.clone().add(0, 1.0, 0);

        world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.6f, 0.8f);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 1.2f, 1.8f);

        int ringPoints = 36;
        for (int i = 0; i < ringPoints; i++) {
            double angle = Math.toRadians((360.0 / ringPoints) * i);
            double x = Math.cos(angle) * 1.15;
            double z = Math.sin(angle) * 1.15;
            world.spawnParticle(Particle.END_ROD, body.clone().add(x, 0, z), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.DUST, loc.clone().add(x, 0.15, z), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(0, 210, 255), 1.2f));
        }
        world.spawnParticle(Particle.END_ROD, body, 18, 0.35, 0.9, 0.35, 0.02);
        world.spawnParticle(Particle.ENCHANTED_HIT, body, 35, 0.5, 0.8, 0.5, 0.15);
        world.spawnParticle(Particle.SWEEP_ATTACK, body, 8, 0.6, 0.6, 0.6, 0.0);

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                ReflectStance.ReflectState current = ReflectStance.getActiveState(player);
                if (current != state || !player.isOnline() || player.isDead()) {
                    if (current == state) {
                        ReflectStance.clearState(player);
                    }
                    cancel();
                    return;
                }

                Location body = player.getLocation().add(0, 1.0, 0);
                double baseAngle = ticksElapsed * 22.0;
                double radius = 0.95;
                for (int i = 0; i < 3; i++) {
                    double rad = Math.toRadians(baseAngle + i * 120.0);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    body.getWorld().spawnParticle(Particle.END_ROD, body.clone().add(x, 0.25, z), 1, 0, 0, 0, 0);
                    body.getWorld().spawnParticle(Particle.DUST, body.clone().add(x, -0.45, z), 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0, 190, 255), 1.0f));
                }
                body.getWorld().spawnParticle(Particle.ENCHANTED_HIT, body, 1, 0.45, 0.55, 0.45, 0.01);

                ticksElapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }
}
