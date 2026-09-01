package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.DamageVulnerabilityManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

public class MyriadTruths implements Ability {

    private final long chargeTimeMillis;
    private final double radius;
    private final int durationSeconds;
    private final double damageBonus;

    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DamageVulnerabilityManager damageVulnerabilityManager;

    public MyriadTruths(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                        TeamManager teamManager, DamageVulnerabilityManager damageVulnerabilityManager,
                        AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.damageVulnerabilityManager = damageVulnerabilityManager;

        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.radius = config.getDouble("radius", 5.0);
        this.durationSeconds = config.getInt("duration-seconds", 7);
        this.damageBonus = config.getDouble("damage-bonus", 0.30);

        this.cost = new UltimateCost(cooldownManager, usageManager, "myriadtruths");
    }

    @Override
    public String getName() {
        return "Myriad Truths";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "myriadtruths", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation();

        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.5f);
        center.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.2f);
        center.getWorld().spawnParticle(Particle.WITCH, center.clone().add(0, 1, 0), 80, 2.5, 1.0, 2.5, 0.1);
        center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, 1, 0), 50, 2.0, 1.0, 2.0, 0.05);

        List<Player> affectedEnemies = new ArrayList<>();

        for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (nearby instanceof Player target) {
                if (target.getGameMode() == GameMode.SPECTATOR || target.isDead()) continue;
                if (!teamManager.isEnemy(player, target)) continue;

                damageVulnerabilityManager.applyVulnerability(target, damageBonus, durationSeconds * 1000L, getName());
                target.sendMessage(MessageUtils.negative() + String.format("§3%s §3unleashed §5Myriad Truths§3! You receive %d%% extra damage!", player.getName(), (int)(damageBonus * 100)));
                affectedEnemies.add(target);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!damageVulnerabilityManager.isActive(target) || !target.isOnline() || target.isDead()) {
                            cancel();
                            return;
                        }

                        Location headLoc = target.getLocation().add(0, 2.2, 0);
                        headLoc.getWorld().spawnParticle(Particle.WITCH, headLoc, 4, 0.2, 0.2, 0.2, 0.02);
                        headLoc.getWorld().spawnParticle(Particle.SMOKE, headLoc, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                }.runTaskTimer(plugin, 0L, 4L);
            }
        }

        if (!affectedEnemies.isEmpty()) {
            player.sendMessage(MessageUtils.positive() + String.format("§3Afflicted §3%d enemy(ies) §3with §5Myriad Truths§3!", affectedEnemies.size()));
        }

        return true;
    }

    @Override
    public String getDescription() {
        return String.format("Unleashes a surge of dark energy, applying %d%% damage vulnerability to all enemies within %d blocks for %ds.",
                (int)(damageBonus * 100), (int)radius, durationSeconds);
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Extra Damage", "+" + (int)(damageBonus * 100) + "%"),
                new AbilityStat("Radius", radius + "m"),
                new AbilityStat("Duration", durationSeconds + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}