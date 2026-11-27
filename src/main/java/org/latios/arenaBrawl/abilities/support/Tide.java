
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
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tide implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final double allyHeal;
    private final double pulseHeal;
    private final int pulses;
    private final long pulseIntervalMs;
    private final double allyRadius;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;
    private final double selfHeal;
    public Tide(
            Plugin plugin,
            CooldownManager cooldownManager,
            TeamManager teamManager,
            PlayerHealthManager healthManager,
            CombatUpgradeManager combatUpgradeManager,
            AbilityConfig config
    ) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 32000L);
        this.allyHeal = config.getDouble("ally-heal", 140.0);
        this.pulseHeal = config.getDouble("pulse-heal", 70.0);
        this.pulses = Math.max(1, config.getInt("pulses", 3));
        this.pulseIntervalMs = Math.max(50L,
                config.getLong("pulse-interval-ms", 600L));
        this.allyRadius = config.getDouble("ally-radius", 12.0);
        this.selfHeal = config.getDouble("self-heal", 120.0);

        this.cost = new CooldownCost(
                cooldownManager, "tide", cooldownMs, combatUpgradeManager
        );

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
        // Initial cast ALWAYS heals the caster
        healthManager.heal(player, selfHeal, getName());

        player.getWorld().spawnParticle(
                Particle.SPLASH,
                player.getLocation().add(0, 1.0, 0),
                25, 0.5, 0.4, 0.5, 0.05
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ITEM_BOTTLE_FILL,
                1.2f, 1.1f
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_WATER_AMBIENT,
                1.0f, 1.4f
        );

        // Caster is marked visited so the tide can NEVER bounce back to them.
        Set<Player> visited = new HashSet<>();
        visited.add(player);

        // Find the first ally to bounce to.
        Player firstAlly = findAlly(player, player, visited);

        // If no ally is nearby, the self-heal still succeeds.
        if (firstAlly == null) {
            return true;
        }

        visited.add(firstAlly);

        // First bounce heals the first ally.
        healthManager.healAlly(player, firstAlly, allyHeal, getName());
        drawWave(player, firstAlly);
        playHealEffects(firstAlly);

        int intervalTicks = (int) Math.max(1L, pulseIntervalMs / 50L);

        new BukkitRunnable() {
            int bouncesDone = 1;
            Player currentTarget = firstAlly;

            @Override
            public void run() {
                if (bouncesDone >= pulses) {
                    cancel();
                    return;
                }

                if (!player.isOnline()
                        || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)
                        || healthManager.isEliminated(player)) {
                    cancel();
                    return;
                }

                // Find a new ally near the last target.
                // Previously healed players and the caster are excluded.
                Player nextAlly = findAlly(player, currentTarget, visited);

                if (nextAlly == null) {
                    cancel();
                    return;
                }

                // Prevent repeat targets.
                visited.add(nextAlly);

                // Visual bounce from previous ally to next ally.
                drawWave(currentTarget, nextAlly);

                // Subsequent bounces heal allies ONLY.
                healthManager.healAlly(
                        player,
                        nextAlly,
                        pulseHeal,
                        getName()
                );

                playHealEffects(nextAlly);

                currentTarget = nextAlly;
                bouncesDone++;
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);

        return true;
    }

    private Player findAlly(Player caster, Player source, Set<Player> visited) {
        return source.getNearbyEntities(allyRadius, allyRadius, allyRadius)
                .stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                // Never bounce to caster
                .filter(candidate -> !candidate.equals(caster))
                // Never bounce to someone already healed
                .filter(candidate -> !visited.contains(candidate))
                .filter(candidate -> candidate.isOnline())
                .filter(candidate -> !candidate.isDead())
                .filter(candidate -> candidate.getGameMode() != GameMode.SPECTATOR)
                .filter(candidate -> config.getMatchManager().isInMatch(candidate))
                .filter(candidate -> !healthManager.isEliminated(candidate))
                .filter(candidate -> teamManager.isAlly(caster, candidate))
                .filter(candidate -> candidate.getWorld().equals(source.getWorld()))
                .filter(candidate ->
                        candidate.getLocation().distanceSquared(source.getLocation())
                                <= allyRadius * allyRadius)
                .min(Comparator.comparingDouble(
                        candidate -> candidate.getLocation()
                                .distanceSquared(source.getLocation())))
                .orElse(null);
    }

    private void playHealEffects(Player target) {
        Location location = target.getLocation().add(0, 1.0, 0);

        target.getWorld().spawnParticle(
                Particle.SPLASH,
                location,
                18, 0.4, 0.5, 0.4, 0.08
        );

        target.getWorld().spawnParticle(
                Particle.BUBBLE,
                location,
                10, 0.3, 0.4, 0.3, 0.05
        );

        target.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                location,
                8, 0.3, 0.5, 0.3, 0.05
        );

        target.getWorld().playSound(
                target.getLocation(),
                Sound.BLOCK_WATER_AMBIENT,
                0.8f, 1.6f
        );

        target.getWorld().playSound(
                target.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.6f, 1.8f
        );
    }


    /**
     * Creates a water projectile effect between two players.
     */
    private void drawWave(Player from, Player to) {
        Location start = from.getLocation().add(0, 1.0, 0);
        Location end = to.getLocation().add(0, 1.0, 0);

        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();

        if (length <= 0) return;

        direction.normalize().multiply(0.35);

        Location current = start.clone();
        double traveled = 0;

        while (traveled < length) {
            current.getWorld().spawnParticle(
                    Particle.SPLASH,
                    current,
                    3,
                    0.08, 0.08, 0.08,
                    0.03
            );

            current.getWorld().spawnParticle(
                    Particle.BUBBLE,
                    current,
                    1,
                    0.04, 0.04, 0.04,
                    0.01
            );

            current.getWorld().spawnParticle(
                    Particle.DOLPHIN,
                    current,
                    1,
                    0.02, 0.02, 0.02,
                    0.01
            );

            current.add(direction);
            traveled += 0.35;
        }
    }

    @Override
    public String getDescription() {
        return "Sends a bouncing healing tide between nearby allies. "
                + "Each ally can only be healed once per cast, "
                + "and the tide never heals you.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("First Bounce Heal",
                        String.valueOf((int) allyHeal)),
                new AbilityStat("Subsequent Heal",
                        String.valueOf((int) pulseHeal)),
                new AbilityStat("Max Bounces",
                        String.valueOf(pulses)),
                new AbilityStat("Bounce Range",
                        (int) allyRadius + " blocks"),
                new AbilityStat("Cooldown",
                        (cooldownMs / 1000L) + "s")
        );
    }
}
