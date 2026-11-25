package org.latios.arenaBrawl.abilities.ultimate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Berserk implements Ability {

    private static final String SPEED_SOURCE = "berserk";

    public static final Set<UUID> BERSERK_ACTIVE_PLAYERS = new HashSet<>();

    /** Speed manager per active berserk, so the boost can be revoked without touching other speed buffs. */
    private static final Map<UUID, SpeedBuffManager> SPEED_MANAGERS = new ConcurrentHashMap<>();

    /** Drops the melee rage state (doubled melee damage), its speed boost and visuals for a player. */
    public static void clear(Player player) {
        BERSERK_ACTIVE_PLAYERS.remove(player.getUniqueId());

        SpeedBuffManager speed = SPEED_MANAGERS.remove(player.getUniqueId());
        if (speed != null) {
            speed.removeBuff(player, SPEED_SOURCE);
        }
    }

    private final long durationMillis;
    private final long chargeTimeMillis;
    private final int speedAmplifier;
    private final AbilityConfig config;
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
        this.config = config;
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

        speedBuffManager.applyBuff(player, speedAmplifier, durationMillis, SPEED_SOURCE);
        SPEED_MANAGERS.put(uuid, speedBuffManager);

        BERSERK_ACTIVE_PLAYERS.add(uuid);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 10.0f, 0.8f);
        spawnAuraCylinder(player);

        new BukkitRunnable() {
            int secondsElapsed = 0;
            int maxSeconds = (int) (durationMillis / 1000L);

            @Override
            public void run() {
                boolean expiredNaturally = secondsElapsed >= maxSeconds;
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !BERSERK_ACTIVE_PLAYERS.contains(uuid)
                        || expiredNaturally) {
                    clear(player);
                    if (expiredNaturally && player.isOnline() && !player.isDead()
                            && player.getGameMode() != GameMode.SPECTATOR) {
                        Component expireMessage = Component.text("Your ", NamedTextColor.YELLOW)
                                .append(Component.text("BERSERK", NamedTextColor.RED, TextDecoration.BOLD))
                                .append(Component.text(" has expired!", NamedTextColor.YELLOW));
                        player.sendMessage(expireMessage);
                    }
                    cancel();
                    return;
                }

                secondsElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private void spawnAuraCylinder(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int circleCount = 12;
        int pointsPerCircle = 24;
        double radius = 1.0;

        double minHeight = 0.1;
        double maxHeight = 2.1;
        double heightStep = (maxHeight - minHeight) / (circleCount - 1);

        for (int i = 0; i < circleCount; i++) {
            double yOffset = minHeight + (i * heightStep);

            for (int j = 0; j < pointsPerCircle; j++) {
                double angle = (2 * Math.PI / pointsPerCircle) * j;
                Location p = loc.clone().add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
                world.spawnParticle(Particle.FLAME, p, 1, 0, 0, 0, 0);
                if (j % 4 == 0) {
                    world.spawnParticle(Particle.LAVA, p, 1, 0, 0, 0, 0);
                }
            }
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
}