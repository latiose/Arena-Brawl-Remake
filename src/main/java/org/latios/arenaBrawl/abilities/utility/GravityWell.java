package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
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
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;


public class GravityWell implements Ability {

    private final double radius;
    private final long durationMs;
    private final double pullStrength;
    private final long cooldownMs;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final AbilityConfig config;

    public GravityWell(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                       CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.teamManager = teamManager;

        this.radius = config.getDouble("radius", 6.0);
        this.durationMs = config.getLong("duration-ms", 5000L);
        this.pullStrength = config.getDouble("pull-strength", 0.5);
        this.cooldownMs = config.getLong("cooldown-ms", 60000L);

        this.cost = new CooldownCost(cooldownManager, "gravitywell", cooldownMs, combatUpgradeManager);
        this.config = config;
    }

    @Override
    public String getName() {
        return "Gravity Well";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Summons a vortex at your position that relentlessly drags enemy players toward its center for " + (durationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Pull", "Strong"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation().clone();

        player.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 10.0f, 0.6f);
        player.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 10.0f, 0.7f);
        player.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(0, 1, 0), 60, 0.5, 1.0, 0.5, 0.5);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed += 2;

                if (ticksElapsed >= durationMs / 50L || !player.isOnline()) {
                    cancel();
                    return;
                }

                drawWell(center, ticksElapsed);

                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius + 2.0, radius)) {
                    if (!(entity instanceof Player victim) || victim.isDead()) continue;
                    if (!teamManager.isEnemy(player, victim)) continue;

                    pull(center, victim);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private void pull(Location center, Player victim) {
        Vector toCenter = center.toVector().subtract(victim.getLocation().toVector());
        toCenter.setY(0);

        double distance = toCenter.length();
        if (distance > radius || distance < 0.5) return;

        // Pull is stronger near the edge, so anyone caught at max range is
        // still dragged in instead of orbiting harmlessly.
        Vector step = toCenter.normalize().multiply(pullStrength);
        step.setY(0.05);
        victim.setVelocity(step);

        victim.getWorld().spawnParticle(Particle.PORTAL,
                victim.getLocation().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0.3);
    }

    private void drawWell(Location center, int ticksElapsed) {
        // Rotating outer ring on the ground marking the area edge.
        double ringAngle = ticksElapsed * 0.15;
        for (int i = 0; i < 24; i++) {
            double angle = ringAngle + (2 * Math.PI / 24) * i;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
            center.getWorld().spawnParticle(Particle.DUST, point, 1,
                    new Particle.DustOptions(Color.fromRGB(110, 0, 180), 1.5f));
        }

        // Rising inner swirl.
        for (double h = 0; h < 3.0; h += 0.5) {
            double angle = ticksElapsed * 0.4 + h * 2.0;
            double swirlRadius = Math.max(0.2, radius * 0.25 * (1 - h / 3.0));
            Location point = center.clone().add(Math.cos(angle) * swirlRadius, h, Math.sin(angle) * swirlRadius);
            center.getWorld().spawnParticle(Particle.PORTAL, point, 3, 0.05, 0.05, 0.05, 0.02);
            center.getWorld().spawnParticle(Particle.SMOKE, point, 1, 0.1, 0.1, 0.1, 0);
        }
    }
}
