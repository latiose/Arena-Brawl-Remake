package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Team ultimate. Deploys a protective dome that makes the caster and every ally inside it
 * immune to damage for a short time while pushing enemies out of the area.
 */
public class Sanctuary implements Ability {

    private final long chargeTimeMillis;
    private final long durationMs;
    private final double radius;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final ShieldManager shieldManager;
    private final AbilityConfig config;

    public Sanctuary(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                     TeamManager teamManager, ShieldManager shieldManager, AbilityConfig config) {
        this.plugin = plugin;
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.durationMs = config.getLong("duration-ms", 4000L);
        this.radius = config.getDouble("radius", 5.0);

        this.cost = new UltimateCost(cooldownManager, usageManager, "sanctuary");
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.shieldManager = shieldManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Sanctuary";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "sanctuary", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Deploys a holy dome that makes you and nearby allies immune to damage while pushing enemies out.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Immunity", (durationMs / 1000.0) + "s"),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Target", "Team bubble"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation().clone();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 10.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 10.0f, 1.2f);

        // Grant immunity once to the caster and every ally currently inside the dome.
        shieldManager.applyShield(player, 1.0, durationMs, "Sanctuary");
        player.sendMessage("§bSanctuary§3 protects you!");

        for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius + 1.0, radius)) {
            if (!(nearby instanceof Player ally)) continue;
            if (ally.equals(player) || ally.isDead()) continue;
            if (!teamManager.isAlly(player, ally)) continue;
            if (ally.getLocation().distanceSquared(center) > radius * radius) continue;

            shieldManager.applyShield(ally, 1.0, durationMs, "Sanctuary");
            ally.sendMessage("§bSanctuary§3 protects you!");
        }

        long ticks = durationMs / 50L;

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= ticks || !player.isOnline()) {
                    cancel();
                    return;
                }

                center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 1.0, 0), 6,
                        radius * 0.4, 1.0, radius * 0.4, 0.01);
                drawDome(center);

                for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius + 1.0, radius)) {
                    if (!(nearby instanceof Player other)) continue;
                    if (other.isDead()) continue;
                    if (teamManager.isEnemy(player, other)) {
                        pushOut(center, other);
                    }
                }

                ticksElapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private void drawDome(Location center) {
        int layers = 5;
        int points = 18;

        for (int layer = 0; layer < layers; layer++) {
            double y = 0.3 + layer * (3.0 / layers);
            double layerRadius = radius * Math.sqrt(Math.max(0.05, 1.0 - Math.pow((y - 1.6) / 2.4, 2)));

            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double x = Math.cos(angle) * layerRadius;
                double z = Math.sin(angle) * layerRadius;
                center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
            }
        }
    }

    private void pushOut(Location center, Player enemy) {
        Vector push = enemy.getLocation().toVector().subtract(center.toVector());
        push.setY(0);
        if (push.lengthSquared() < 0.0001) {
            push = new Vector(1, 0, 0);
        }
        push.normalize().multiply(0.9);
        push.setY(0.25);
        enemy.setVelocity(push);
        enemy.getWorld().spawnParticle(Particle.CLOUD, enemy.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.02);
    }
}
