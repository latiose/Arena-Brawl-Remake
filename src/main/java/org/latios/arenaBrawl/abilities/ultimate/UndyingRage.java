package org.latios.arenaBrawl.abilities.ultimate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.EnergyManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UndyingRage implements Ability {

    private static final Map<UUID, Double> ACTIVE_FLOORS = new ConcurrentHashMap<>();

    public static void clear(Player player) {
        ACTIVE_FLOORS.remove(player.getUniqueId());
    }

    public static boolean isActive(Player player) {
        return ACTIVE_FLOORS.containsKey(player.getUniqueId());
    }

    public static double getHealthFloor(Player player) {
        return ACTIVE_FLOORS.getOrDefault(player.getUniqueId(), 0.0);
    }

    private final long durationMillis;
    private final long chargeTimeMillis;
    private final double energyGain;
    private final double healthFloor;
    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final EnergyManager energyManager;
    private final AbilityCost cost;

    public UndyingRage(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                       EnergyManager energyManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.energyManager = energyManager;
        this.durationMillis = config.getLong("duration-ms", 5000L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.energyGain = config.getDouble("energy-gain", 50.0);
        this.healthFloor = config.getDouble("health-floor", 1.0);
        this.cost = new UltimateCost(cooldownManager, usageManager, "undyingrage");
    }

    @Override
    public String getName() {
        return "Undying Rage";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "undyingrage", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Becomes unable to die for 5 seconds, refusing to be reduced below 1 health and instantly gaining 50 energy.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Death Immunity", (durationMillis / 1000) + "s"),
                new AbilityStat("Health Floor", String.valueOf((int) healthFloor)),
                new AbilityStat("Energy Gained", String.valueOf((int) energyGain)),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        UUID uuid = player.getUniqueId();

        ACTIVE_FLOORS.put(uuid, healthFloor);
        energyManager.addEnergy(player, energyGain);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 10.0f, 0.9f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 6.0f, 1.4f);

        spawnBurst(player);

        new BukkitRunnable() {
            int secondsElapsed = 0;
            final int maxSeconds = (int) (durationMillis / 1000L);

            @Override
            public void run() {
                boolean expiredNaturally = secondsElapsed >= maxSeconds;
                if (!player.isOnline() || player.isDead()
                        || !ACTIVE_FLOORS.containsKey(uuid)
                        || expiredNaturally) {
                    ACTIVE_FLOORS.remove(uuid);
                    if (expiredNaturally && player.isOnline()) {
                        player.sendMessage(Component.text("Your ", NamedTextColor.YELLOW)
                                .append(Component.text("UNDYING RAGE", NamedTextColor.RED, TextDecoration.BOLD))
                                .append(Component.text(" has expired!", NamedTextColor.YELLOW)));
                    }
                    cancel();
                    return;
                }

                spawnAura(player);
                secondsElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private void spawnAura(Player player) {
        Location loc = player.getLocation();
        int points = 18;
        double radius = 0.9;
        double spin = Math.toRadians(player.getTicksLived() * 12.0);

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i + spin;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = loc.clone().add(x, 0.2, z);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, particleLoc, 1, 0, 0.05, 0, 0.02);
            player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(200, 30, 30), 1.0f));
        }
    }

    private void spawnBurst(Player player) {
        Location loc = player.getLocation();
        int circles = 6;
        int points = 16;
        double radius = 1.2;

        for (int c = 0; c < circles; c++) {
            double yOffset = 0.2 + c * 0.35;

            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location particleLoc = loc.clone().add(x, yOffset, z);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, particleLoc, 1, 0, 0, 0, 0.03);
            }
        }
    }
}
