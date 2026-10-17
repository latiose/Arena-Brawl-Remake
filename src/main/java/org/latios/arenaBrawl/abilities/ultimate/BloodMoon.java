package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BloodMoon implements Ability {

    private record Active(UUID casterId, long endMs, double radius, double lifestealFraction, TeamManager teamManager) {}

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    private final double radius;
    private final long durationMs;
    private final double lifestealPercent;
    private final long antihealRefreshMs;
    private final long chargeTimeMillis;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public BloodMoon(CooldownManager cooldownManager, UsageManager usageManager, TeamManager teamManager,
                     DebuffManager debuffManager, AbilityConfig config) {
        this.radius = config.getDouble("radius", 8.0);
        this.durationMs = config.getLong("duration-ms", 8000L);
        this.lifestealPercent = config.getDouble("lifesteal-percent", 40.0);
        this.antihealRefreshMs = config.getLong("antiheal-refresh-ms", 1500L);
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "bloodmoon");
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Blood Moon";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "bloodmoon", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "For " + (durationMs / 1000L) + "s, you and nearby allies heal for "
                + (int) lifestealPercent + "% of damage dealt. Enemies in range cannot heal.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Lifesteal", (int) lifestealPercent + "%"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        UUID id = player.getUniqueId();
        Active active = new Active(
                id,
                System.currentTimeMillis() + durationMs,
                radius,
                lifestealPercent / 100.0,
                teamManager
        );
        ACTIVE.put(id, active);

        MatchSoundUtils.play(config, player, Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.6f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
        player.sendMessage(MessageUtils.positive() + "§3The Blood Moon rises!");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (ACTIVE.get(id) != active) {
                    cancel();
                    return;
                }
                if (!player.isOnline() || player.isDead() || System.currentTimeMillis() >= active.endMs()) {
                    ACTIVE.remove(id, active);
                    if (player.isOnline()) {
                        player.sendMessage(MessageUtils.negative() + "§3The Blood Moon fades.");
                    }
                    cancel();
                    return;
                }

                Location center = player.getLocation();
                drawRing(center);
                center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 1, 0),
                        8, 0.5, 0.8, 0.5, 0, new Particle.DustOptions(Color.fromRGB(160, 0, 20), 1.3f));

                for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (!(nearby instanceof Player enemy)) continue;
                    if (enemy.equals(player) || enemy.isDead()) continue;
                    if (enemy.getGameMode() == GameMode.SPECTATOR) continue;
                    if (!teamManager.isEnemy(player, enemy)) continue;
                    if (enemy.getLocation().distanceSquared(center) > radius * radius) continue;
                    if (debuffManager.hasDebuff(enemy, DebuffType.ANTIHEAL)) continue;

                    debuffManager.tryApply(player, enemy, DebuffType.ANTIHEAL, antihealRefreshMs, 100.0);
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 10L);

        return true;
    }

    public static void onDamageDealt(Player attacker, Player victim, double damage,
                                     PlayerHealthManager healthManager) {
        if (attacker == null || attacker.equals(victim) || damage <= 0) return;

        long now = System.currentTimeMillis();
        for (Active active : ACTIVE.values()) {
            if (now >= active.endMs()) continue;
            Player caster = Bukkit.getPlayer(active.casterId());
            if (caster == null || !caster.isOnline() || caster.isDead()) continue;
            // Instances live in separate worlds; never mix matches and never
            // measure distance across worlds (that throws).
            if (!attacker.getWorld().equals(caster.getWorld())) continue;
            if (!attacker.equals(caster) && !active.teamManager().isAlly(caster, attacker)) continue;
            if (attacker.getLocation().distanceSquared(caster.getLocation()) > active.radius() * active.radius()) {
                continue;
            }

            healthManager.heal(attacker, damage * active.lifestealFraction(), "Blood Moon");
            attacker.getWorld().spawnParticle(Particle.DUST, attacker.getLocation().add(0, 1.8, 0),
                    4, 0.25, 0.15, 0.25, 0, new Particle.DustOptions(Color.fromRGB(180, 0, 30), 1.1f));
            return;
        }
    }

    private void drawRing(Location center) {
        int points = 28;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            Location point = center.clone().add(radius * Math.cos(angle), 0.15, radius * Math.sin(angle));
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(140, 0, 20), 1.2f));
        }
    }
}
