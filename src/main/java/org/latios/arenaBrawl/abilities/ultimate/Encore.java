package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Encore implements Ability {

    private static final Map<UUID, Long> LOCKED_UNTIL = new HashMap<>();

    private final double maxRange;
    private final long chargeTimeMillis;
    private final long lockMillis;
    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final UsageManager usageManager;
    private final TeamManager teamManager;
    private final AbilityCost cost;

    public Encore(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                  TeamManager teamManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.usageManager = usageManager;
        this.teamManager = teamManager;

        this.maxRange = config.getDouble("max-range", 25.0);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.lockMillis = config.getLong("lock-ms", 8000L);

        this.cost = new UltimateCost(cooldownManager, usageManager, "encore");
    }

    @Override
    public String getName() {
        return "Encore";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "encore", chargeTimeMillis);
        LOCKED_UNTIL.remove(player.getUniqueId());
    }

    @Override
    public String getDescription() {
        return "Resets the cooldowns of a targeted ally, including their ultimate, letting them perform one more time.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Resets", "Ultimate, utility and support"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        if (isLocked(player)) {
            return false;
        }

        Player target = AbilityTargeting.findAllyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        cooldownManager.clearPlayer(target);
        usageManager.resetPlayer(target);

        LOCKED_UNTIL.put(player.getUniqueId(), System.currentTimeMillis() + lockMillis);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 10.0f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10.0f, 1.2f);
        target.getWorld().spawnParticle(Particle.NOTE, target.getLocation().add(0, 2.0, 0), 25, 0.5, 0.4, 0.5, 1.0);
        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1.0, 0), 30, 0.4, 0.7, 0.4, 0.05);

        drawEncore(target);

        target.sendMessage("§eEncore§e! Your abilities and ultimate have been reset.");

        return true;
    }

    private boolean isLocked(Player player) {
        Long until = LOCKED_UNTIL.get(player.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            LOCKED_UNTIL.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    private void drawEncore(Player target) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!target.isOnline() || target.isDead() || ticks >= 40) {
                    cancel();
                    return;
                }

                double angle = ticks * 0.6;
                double radius = 1.1;

                for (int i = 0; i < 3; i++) {
                    double currentAngle = angle + (i * (2 * Math.PI / 3));
                    double x = Math.cos(currentAngle) * radius;
                    double z = Math.sin(currentAngle) * radius;

                    target.getWorld().spawnParticle(
                            Particle.DUST,
                            target.getLocation().add(x, 0.4 + (ticks * 0.03), z),
                            1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 105, 220), 1.1f)
                    );
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
