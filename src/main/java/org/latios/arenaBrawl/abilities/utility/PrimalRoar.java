package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class PrimalRoar implements Ability {

    private final long cooldownMs;
    private final double shieldPercent;
    private final long shieldDurationMs;
    private final double radius;
    private final long silenceDurationMs;

    private final AbilityCost cost;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final TeamManager teamManager;
    private final Plugin plugin;
    private final AbilityConfig config;

    public PrimalRoar(Plugin plugin, CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                      ShieldManager shieldManager, DebuffManager debuffManager, TeamManager teamManager,
                      AbilityConfig config) {
        this.plugin = plugin;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.teamManager = teamManager;
        this.config = config;

        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.shieldPercent = config.getDouble("shield-percent", 0.30);
        this.shieldDurationMs = config.getLong("shield-duration-ms", 3000L);
        this.radius = config.getDouble("radius", 3.0);
        this.silenceDurationMs = config.getLong("silence-duration-ms", 2000L);

        this.cost = new CooldownCost(cooldownManager, "primalroar", cooldownMs, upgradeManager);
    }

    @Override
    public String getName() {
        return "Primal Roar";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Gains a temporary shield and lets out a roar when it expires, silencing nearby enemies.";
    }

    @Override
    public List getStats() {
        return List.of(
                new AbilityStat("Shield", (int) (shieldPercent * 100) + "%"),
                new AbilityStat("Silence", (silenceDurationMs / 1000.0) + "s"),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        shieldManager.applyShield(player, shieldPercent, shieldDurationMs, "PRIMAL_ROAR");

        MatchSoundUtils.play(config, player, Sound.ENTITY_WOLF_GROWL, 1.0f, 0.8f);

        BukkitTask particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1.5, 0), 6, 0.4, 0.4, 0.4, 0.0);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        long delayTicks = shieldDurationMs / 50L;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            particleTask.cancel();

            if (!player.isOnline() || player.isDead()) return;

            Location center = player.getLocation();

            MatchSoundUtils.play(config, player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);

            int points = 32;
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location particleLoc = center.clone().add(x, 0.2, z);
                center.getWorld().spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
                center.getWorld().spawnParticle(Particle.SQUID_INK, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
            }

            for (Player enemy : center.getWorld().getPlayers()) {
                if (enemy.equals(player) || !teamManager.isEnemy(player, enemy) || enemy.isDead()) continue;

                if (enemy.getLocation().distance(center) <= radius) {
                    debuffManager.tryApply(enemy, DebuffType.SILENCE, silenceDurationMs);
                    MatchSoundUtils.play(config, enemy, Sound.ENTITY_BAT_TAKEOFF, 0.8f, 0.6f);
                    enemy.getWorld().spawnParticle(Particle.SMOKE, enemy.getLocation().add(0, 1.0, 0), 20, 0.3, 0.5, 0.3, 0.05);
                }
            }
        }, delayTicks);

        return true;
    }
}