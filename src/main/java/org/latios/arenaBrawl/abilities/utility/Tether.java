package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Links the caster to an enemy for a few seconds. If the target stretches the
 * chain too far, it is pulled back toward the caster and slowed.
 */
public class Tether implements Ability {

    private final long cooldownMs;
    private final double maxRange;
    private final double breakDistance;
    private final long durationMs;
    private final double pullStrength;
    private final long slowDurationMs;
    private final int tickInterval;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public Tether(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                  DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager,
                  AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.maxRange = config.getDouble("max-range", 20.0);
        this.breakDistance = config.getDouble("break-distance", 8.0);
        this.durationMs = config.getLong("duration-ms", 3000L);
        this.pullStrength = config.getDouble("pull-strength", 1.1);
        this.slowDurationMs = config.getLong("slow-duration-ms", 1000L);
        this.tickInterval = Math.max(1, config.getInt("tick-interval-ticks", 5));

        this.cost = new CooldownCost(cooldownManager, "tether", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Tether";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player target = findTarget(player);
        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        player.sendMessage(MessageUtils.positive() + "§3Tether connected to §c" + target.getName() + "§3!");
        target.sendMessage(MessageUtils.negative() + "§3You are tethered to §c" + player.getName() + "§3!");

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.2f, 1.3f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.9f);

        long endAt = System.currentTimeMillis() + durationMs;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= endAt
                        || !player.isOnline() || !target.isOnline()
                        || player.isDead() || target.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || target.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)
                        || !config.getMatchManager().isInMatch(target)) {
                    cancel();
                    return;
                }

                drawTether(player.getEyeLocation(), target.getLocation().add(0, 1.0, 0));

                double distance = player.getLocation().distance(target.getLocation());
                if (distance <= breakDistance) return;

                Vector pull = player.getLocation().toVector()
                        .subtract(target.getLocation().toVector());
                if (pull.lengthSquared() < 0.01) return;

                pull.normalize().multiply(pullStrength);
                pull.setY(0.18);
                target.setVelocity(pull);

                debuffManager.tryApply(player, target, DebuffType.SLOW, slowDurationMs, 0.0);

                target.getWorld().spawnParticle(
                        Particle.CRIT, target.getLocation().add(0, 1.0, 0), 8,
                        0.25, 0.3, 0.25, 0.03
                );
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 0.8f, 1.4f);
            }
        }.runTaskTimer(plugin, 0L, tickInterval);

        return true;
    }

    private Player findTarget(Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTrace(
                eye, eye.getDirection().normalize(), maxRange,
                FluidCollisionMode.NEVER, true, 0.45,
                entity -> entity instanceof Player candidate
                        && !candidate.equals(player)
                        && !candidate.isDead()
                        && candidate.getGameMode() != GameMode.SPECTATOR
                        && teamManager.isEnemy(player, candidate)
        );

        return hit != null && hit.getHitEntity() instanceof Player target ? target : null;
    }

    private void drawTether(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length <= 0) return;

        direction.normalize().multiply(0.35);
        Location current = start.clone();
        double traveled = 0;
        while (traveled < length) {
            current.getWorld().spawnParticle(Particle.END_ROD, current, 1, 0, 0, 0, 0);
            current.add(direction);
            traveled += 0.35;
        }
    }

    @Override
    public String getDescription() {
        return "Links you to an enemy for " + (durationMs / 1000L) +
                "s. If they move more than " + (int) breakDistance +
                " blocks away, they are pulled back and slowed.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Range", String.format("%.1f blocks", maxRange)),
                new AbilityStat("Break Distance", String.format("%.1f blocks", breakDistance)),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Slow", (slowDurationMs / 1000L) + "s")
        );
    }
}
