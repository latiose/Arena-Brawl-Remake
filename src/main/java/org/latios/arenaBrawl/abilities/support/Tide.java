package org.latios.arenaBrawl.abilities.support;

import org.bukkit.GameMode;
import org.bukkit.Location;
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
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.Comparator;
import java.util.List;

public class Tide implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final double selfHeal;
    private final double allyHeal;
    private final double pulseHeal;
    private final int pulses;
    private final long pulseIntervalMs;
    private final double allyRadius;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Tide(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                PlayerHealthManager healthManager, CombatUpgradeManager combatUpgradeManager,
                AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 32000L);
        this.selfHeal = config.getDouble("self-heal", 120.0);
        this.allyHeal = config.getDouble("ally-heal", 140.0);
        this.pulseHeal = config.getDouble("pulse-heal", 70.0);
        this.pulses = Math.max(1, config.getInt("pulses", 3));
        this.pulseIntervalMs = Math.max(50L, config.getLong("pulse-interval-ms", 600L));
        this.allyRadius = config.getDouble("ally-radius", 12.0);

        this.cost = new CooldownCost(cooldownManager, "tide", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Tide";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player ally = findAlly(player);

        healthManager.heal(player, selfHeal, getName());
        if (ally != null) {
            healthManager.healAlly(player, ally, allyHeal, getName());
            drawWave(player, ally);
        }

        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1.0, 0),
                25, 0.5, 0.4, 0.5, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.2f, 1.1f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.4f);

        int intervalTicks = (int) Math.max(1L, pulseIntervalMs / 50L);

        new BukkitRunnable() {
            int done = 0;

            @Override
            public void run() {
                if (done >= pulses) {
                    cancel();
                    return;
                }

                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)
                        || healthManager.isEliminated(player)) {
                    cancel();
                    return;
                }

                Player currentAlly = findAlly(player);
                healthManager.heal(player, pulseHeal);

                player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1.0, 0),
                        10, 0.4, 0.4, 0.4, 0.03);

                if (currentAlly != null) {
                    healthManager.healAlly(player, currentAlly, pulseHeal, getName());
                    drawWave(player, currentAlly);
                    currentAlly.getWorld().playSound(currentAlly.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.8f, 1.6f);
                }

                done++;
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);

        return true;
    }

    private Player findAlly(Player player) {
        return player.getNearbyEntities(allyRadius, allyRadius, allyRadius).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .filter(candidate -> !candidate.equals(player))
                .filter(candidate -> !candidate.isDead())
                .filter(candidate -> candidate.getGameMode() != GameMode.SPECTATOR)
                .filter(candidate -> teamManager.isAlly(player, candidate))
                .min(Comparator.comparingDouble(candidate -> candidate.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    private void drawWave(Player player, Player ally) {
        Location start = player.getLocation().add(0, 1.0, 0);
        Location end = ally.getLocation().add(0, 1.0, 0);

        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length <= 0) return;

        direction.normalize().multiply(0.4);
        Location current = start.clone();
        double traveled = 0;

        while (traveled < length) {
            current.getWorld().spawnParticle(Particle.SPLASH, current, 2, 0.08, 0.08, 0.08, 0.01);
            current.getWorld().spawnParticle(Particle.BUBBLE, current, 1, 0.05, 0.05, 0.05, 0.0);
            current.add(direction);
            traveled += 0.4;
        }
    }

    @Override
    public String getDescription() {
        return "Releases a healing tide that washes over you and a nearby ally, healing both instantly and in a few following waves.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Self Heal", String.valueOf((int) selfHeal)),
                new AbilityStat("Ally Heal", String.valueOf((int) allyHeal)),
                new AbilityStat("Wave Heal", (int) pulseHeal + " x" + pulses),
                new AbilityStat("Ally Radius", (int) allyRadius + " blocks"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
