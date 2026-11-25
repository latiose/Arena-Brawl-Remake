package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.UsableWhileDisabled;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Purify implements Ability, UsableWhileDisabled {

    private static final long AURA_INTERVAL_TICKS = 10L;

    private final Plugin plugin;
    private final long cooldownMs;
    private final long durationMs;
    private final double radius;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    public Purify(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                  DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager,
                  AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 26000L);
        this.durationMs = config.getLong("duration-ms", 4000L);
        this.radius = config.getDouble("radius", 8.0);

        this.cost = new CooldownCost(cooldownManager, "purify", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() {
        return "Purify";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Set<Player> targets = new LinkedHashSet<>();
        targets.add(player);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player candidate)) continue;
            if (candidate.equals(player) || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isAlly(player, candidate)) continue;
            if (candidate.getLocation().distanceSquared(player.getLocation()) > radius * radius) continue;
            targets.add(candidate);
        }

        for (Player target : targets) {
            debuffManager.clear(target);
            debuffManager.grantDebuffImmunity(target, durationMs);
            target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1.0, 0),
                    20, 0.35, 0.5, 0.35, 0.05);
            target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1.0, 0),
                    15, 0.3, 0.5, 0.3, 0.4);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        }

        drawRing(player.getLocation().add(0, 0.2, 0), radius);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.4f);

        startAura(targets);
        return true;
    }

    private void startAura(Set<Player> targets) {
        long durationTicks = Math.max(1L, durationMs / 50L);

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= durationTicks) {
                    cancel();
                    return;
                }

                for (Player target : new ArrayList<>(targets)) {
                    if (!target.isOnline() || target.isDead()) continue;

                    Location center = target.getLocation().add(0, 1.0, 0);
                    center.getWorld().spawnParticle(Particle.END_ROD, center,
                            2, 0.25, 0.35, 0.25, 0.01);
                    center.getWorld().spawnParticle(Particle.ENCHANT, center,
                            1, 0.2, 0.3, 0.2, 0.3);
                }

                ticksElapsed += AURA_INTERVAL_TICKS;
            }
        }.runTaskTimer(plugin, AURA_INTERVAL_TICKS, AURA_INTERVAL_TICKS);
    }

    private void drawRing(Location center, double ringRadius) {
        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i;
            Location point = center.clone().add(Math.cos(angle) * ringRadius, 0.0, Math.sin(angle) * ringRadius);
            point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0.01);
        }
    }

    @Override
    public String getDescription() {
        return "Cleanses debuffs and grants you and all allies within " + (int) radius + " blocks immunity to debuffs for "
                + (durationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Immunity Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Target", "You and allies"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
