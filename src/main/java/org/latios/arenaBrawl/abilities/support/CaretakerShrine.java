package org.latios.arenaBrawl.abilities.support;

import org.bukkit.GameMode;
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
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class CaretakerShrine implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final double placementDistance;
    private final double pickupRadius;
    private final long chargeTimeMs;
    private final long durationMs;
    private final double minHeal;
    private final double maxHeal;
    private final int speedAmplifier;
    private final long speedDurationMs;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final SpeedBuffManager speedBuffManager;
    private final AbilityConfig config;

    public CaretakerShrine(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                           PlayerHealthManager healthManager, SpeedBuffManager speedBuffManager,
                           CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 22000L);
        this.placementDistance = config.getDouble("placement-distance", 2.0);
        this.pickupRadius = config.getDouble("pickup-radius", 1.6);
        this.chargeTimeMs = config.getLong("charge-time-ms", 8000L);
        this.durationMs = config.getLong("duration-ms", 12000L);
        this.minHeal = config.getDouble("min-heal", 80.0);
        this.maxHeal = config.getDouble("max-heal", 300.0);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.speedDurationMs = config.getLong("speed-duration-ms", 3000L);

        this.cost = new CooldownCost(cooldownManager, "caretakershrine", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.speedBuffManager = speedBuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Caretaker's Shrine";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location center = resolveCenter(player);
        long placedAt = System.currentTimeMillis();

        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.3f);
        center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 0.4, 0),
                15, 0.25, 0.3, 0.25, 0.02);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !config.getMatchManager().isInMatch(player)) {
                    cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - placedAt;
                if (elapsed >= durationMs) {
                    cancel();
                    return;
                }

                Player collector = findCollector(player, center);
                if (collector != null) {
                    collect(player, collector, center, elapsed);
                    cancel();
                    return;
                }

                drawShrine(center, elapsed);
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private Location resolveCenter(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() <= 0) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize().multiply(placementDistance);
        return player.getLocation().clone().add(direction);
    }

    private Player findCollector(Player caster, Location center) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, pickupRadius, pickupRadius, pickupRadius)) {
            if (!(entity instanceof Player candidate)) continue;
            if (candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!candidate.equals(caster) && !teamManager.isAlly(caster, candidate)) continue;
            if (candidate.getLocation().distanceSquared(center) > pickupRadius * pickupRadius) continue;
            return candidate;
        }
        return null;
    }

    private void collect(Player caster, Player collector, Location center, long elapsed) {
        double progress = Math.min(1.0, (double) elapsed / Math.max(1L, chargeTimeMs));
        double healAmount = minHeal + (maxHeal - minHeal) * progress;

        if (collector.equals(caster)) {
            healthManager.heal(collector, healAmount, getName());
        } else {
            healthManager.healAlly(caster, collector, healAmount, getName());
        }
        speedBuffManager.applyBuff(collector, speedAmplifier, speedDurationMs, getName());

        center.getWorld().spawnParticle(Particle.HEART, center.clone().add(0, 0.8, 0),
                10, 0.3, 0.4, 0.3, 0.02);
        center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(0, 0.8, 0),
                22, 0.3, 0.4, 0.3, 0.06);
        collector.getWorld().spawnParticle(Particle.END_ROD, collector.getLocation().add(0, 1.0, 0),
                18, 0.3, 0.5, 0.3, 0.03);
        collector.getWorld().playSound(collector.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.2f, 1.4f);
        collector.getWorld().playSound(collector.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.5f);
    }

    private void drawShrine(Location center, long elapsed) {
        double progress = Math.min(1.0, (double) elapsed / Math.max(1L, chargeTimeMs));
        double ringRadius = 0.35 + progress * 0.45;
        int points = 12;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i + elapsed / 350.0;
            Location point = center.clone().add(
                    Math.cos(angle) * ringRadius, 0.1 + progress * 0.35, Math.sin(angle) * ringRadius);
            point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0.005);
        }

        center.getWorld().spawnParticle(Particle.ENCHANT, center.clone().add(0, 0.6 + progress * 0.4, 0),
                3, 0.2, 0.3, 0.2, 0.2);

        if (progress >= 1.0) {
            center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(0, 0.9, 0),
                    2, 0.15, 0.2, 0.15, 0.02);
        }
    }

    @Override
    public String getDescription() {
        return "Places a shrine that charges up over " + (chargeTimeMs / 1000L)
                + "s. The first ally to pick it up is healed for more the longer it charged and gains Speed II.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", (int) minHeal + " - " + (int) maxHeal),
                new AbilityStat("Charge Time", (chargeTimeMs / 1000L) + "s"),
                new AbilityStat("Shrine Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Speed", "Speed II for " + (speedDurationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
