package org.latios.arenaBrawl.abilities.ultimate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Berserk implements Ability {

    public static final Set<UUID> BERSERK_ACTIVE_PLAYERS = new HashSet<>();

    private final long durationMillis;
    private final long chargeTimeMillis;
    private final int speedAmplifier;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final SpeedBuffManager speedBuffManager;

    public Berserk(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager, SpeedBuffManager speedBuffManager,
                   AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.speedBuffManager = speedBuffManager;

        this.durationMillis = config.getLong("duration-ms", 15000L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);

        this.cost = new UltimateCost(cooldownManager, usageManager, "berserk");
    }

    @Override
    public String getName() {
        return "Berserk";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "berserk", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        UUID uuid = player.getUniqueId();

        speedBuffManager.applyBuff(player, speedAmplifier, durationMillis);

        BERSERK_ACTIVE_PLAYERS.add(uuid);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.8f);
        spawnFlameRings(player);
        spawnLavaRing(player);

        new BukkitRunnable() {
            int secondsElapsed = 0;
            int maxSeconds = (int) (durationMillis / 1000L);

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || secondsElapsed >= maxSeconds) {
                    BERSERK_ACTIVE_PLAYERS.remove(uuid);
                    Component expireMessage = Component.text("Your ", NamedTextColor.YELLOW)
                            .append(Component.text("BERSERK", NamedTextColor.RED, TextDecoration.BOLD))
                            .append(Component.text(" has expired!", NamedTextColor.YELLOW));
                    player.sendMessage(expireMessage);
                    cancel();
                    return;
                }

                spawnLavaRing(player);
                secondsElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private void spawnLavaRing(Player player) {
        Location loc = player.getLocation();
        int points = 12;
        double radius = 1.0;

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = loc.clone().add(x, 0.2, z);
            player.getWorld().spawnParticle(Particle.LAVA, particleLoc, 3, 0, 0, 0, 0);
        }
    }

    @Override
    public String getDescription() {
        return "Enters a rage state, doubling melee damage and boosting speed";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage Increase", "+100%"),
                new AbilityStat("Bonus Speed", "Speed II"),
                new AbilityStat("Duration", (durationMillis / 1000) + "s"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    private void spawnFlameRings(Player player) {
        Location loc = player.getLocation();
        int circleCount = 8;
        int pointsPerCircle = 15;
        double radius = 1.3;

        double minHeight = 0.2;
        double maxHeight = 2.2;
        double heightStep = (maxHeight - minHeight) / (circleCount - 1);

        for (int i = 0; i < circleCount; i++) {
            double yOffset = minHeight + (i * heightStep);

            for (int j = 0; j < pointsPerCircle; j++) {
                double angle = (2 * Math.PI / pointsPerCircle) * j;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location particleLoc = loc.clone().add(x, yOffset, z);
                player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }
}