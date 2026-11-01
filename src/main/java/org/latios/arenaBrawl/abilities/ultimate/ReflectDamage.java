package org.latios.arenaBrawl.abilities.ultimate;

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
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;

import java.util.List;

public class ReflectDamage implements Ability, ReflectStance {

    private final long durationMillis;
    private final double reflectPercent;
    private final double damageReduction;
    private final long chargeTimeMillis;
    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final AbilityCost cost;

    public ReflectDamage(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.durationMillis = config.getLong("duration-ms", 9000L);
        this.reflectPercent = config.getDouble("reflect-percent", 0.50);
        this.damageReduction = config.getDouble("damage-reduction", 0.50);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);

        this.cost = new UltimateCost(cooldownManager, usageManager, "reflectdamage");
    }

    @Override
    public String getName() {
        return "Reflect Damage";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "reflectdamage", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Wraps yourself in a reflecting aura. While it lasts, all incoming damage is halved "
                + "and half of the damage dealt to you is returned to the attacker.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Reflect", (int) (reflectPercent * 100) + "% of damage taken"),
                new AbilityStat("Damage Reduction", (int) (damageReduction * 100) + "%"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000L) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        ReflectStance.ReflectState state = new ReflectStance.ReflectState(
                System.currentTimeMillis() + durationMillis, reflectPercent, damageReduction, getName());
        ReflectStance.apply(player, state);

        Location loc = player.getLocation();
        var world = loc.getWorld();
        Location body = loc.clone().add(0, 1.0, 0);

        world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.6f, 0.7f);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
        world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.6f);

        int ringPoints = 36;
        for (int i = 0; i < ringPoints; i++) {
            double angle = Math.toRadians((360.0 / ringPoints) * i);
            double x = Math.cos(angle) * 1.25;
            double z = Math.sin(angle) * 1.25;
            world.spawnParticle(Particle.END_ROD, body.clone().add(x, 0, z), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.DUST, loc.clone().add(x, 0.15, z), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 40, 255), 1.2f));
        }
        world.spawnParticle(Particle.END_ROD, body, 18, 0.35, 0.9, 0.35, 0.02);
        world.spawnParticle(Particle.WAX_ON, body, 40, 0.5, 0.9, 0.5, 0.08);
        world.spawnParticle(Particle.ENCHANTED_HIT, body, 35, 0.5, 0.8, 0.5, 0.15);

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
                double baseAngle = ticksElapsed * 18.0;
                double radius = 1.0;
                for (int i = 0; i < 3; i++) {
                    double rad = Math.toRadians(baseAngle + i * 120.0);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    body.getWorld().spawnParticle(Particle.WAX_ON, body.clone().add(x, 0.25, z), 1, 0, 0, 0, 0);
                    body.getWorld().spawnParticle(Particle.END_ROD, body.clone().add(x, -0.45, z), 1, 0, 0, 0, 0);
                }
                body.getWorld().spawnParticle(Particle.ENCHANTED_HIT, body, 1, 0.45, 0.55, 0.45, 0.01);

                ticksElapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }
}
