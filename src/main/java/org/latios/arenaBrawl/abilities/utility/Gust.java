package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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

public class Gust implements Ability {

    private final double range;
    private final double coneAngleDegrees;
    private final double knockbackStrength;
    private final long cooldownMs;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final AbilityConfig config;

    public Gust(CooldownManager cooldownManager, TeamManager teamManager,
                CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.range = config.getDouble("range", 7.0);
        this.coneAngleDegrees = config.getDouble("cone-angle-degrees", 70.0);
        this.knockbackStrength = config.getDouble("knockback-strength", 2.1);
        this.cooldownMs = config.getLong("cooldown-ms", 22000L);
        this.cost = new CooldownCost(cooldownManager, "gust", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Gust";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Unleashes a cone of wind that knocks enemies away from you. Deals no damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", range + " blocks"),
                new AbilityStat("Knockback", "Strong"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location origin = player.getEyeLocation();
        Vector look = origin.getDirection();
        if (look.lengthSquared() < 0.0001) {
            look = new Vector(0, 0, 1);
        }
        look.normalize();
        double minDot = Math.cos(Math.toRadians(coneAngleDegrees / 2.0));

        spawnCone(origin, look);

        for (Entity nearby : player.getWorld().getNearbyEntities(origin, range, range, range)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.equals(player) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, target)) continue;

            Vector toTarget = target.getLocation().add(0, 1.0, 0).toVector().subtract(origin.toVector());
            double distance = toTarget.length();
            if (distance > range || distance < 0.0001) continue;
            if (look.dot(toTarget.normalize()) < minDot) continue;

            Vector push = look.clone().multiply(knockbackStrength);
            push.setY(Math.max(0.35, push.getY() * 0.35 + 0.35));
            target.setVelocity(push);
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 8, 0.3, 0.2, 0.3, 0.02);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 10.0f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 10.0f, 1.6f);
        return true;
    }

    private void spawnCone(Location origin, Vector look) {
        for (double d = 0.8; d <= range; d += 0.7) {
            double radius = d * Math.tan(Math.toRadians(coneAngleDegrees / 2.0)) * 0.55;
            Location center = origin.clone().add(look.clone().multiply(d));
            center.getWorld().spawnParticle(Particle.CLOUD, center, 3, radius * 0.35, 0.08, radius * 0.35, 0.01);
            center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 1, 0, 0, 0, 0);
        }
    }
}
