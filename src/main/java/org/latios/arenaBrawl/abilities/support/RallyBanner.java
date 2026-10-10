package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class RallyBanner implements Ability {

    private final AbilityCost cost;
    private final Plugin plugin;
    private final TeamManager teams;
    private final DamageBuffManager damage;
    private final SpeedBuffManager speed;
    private final PlayerHealthManager health;
    private final double radius;
    private final double multiplier;
    private final double healPerSecond;
    private final long duration;
    private final int speedAmp;

    public RallyBanner(
            Plugin plugin,
            CooldownManager cooldowns,
            CombatUpgradeManager upgrades,
            TeamManager teams,
            DamageBuffManager damage,
            SpeedBuffManager speed,
            PlayerHealthManager health,
            AbilityConfig config
    ) {
        this.plugin = plugin;
        this.teams = teams;
        this.damage = damage;
        this.speed = speed;
        this.health = health;

        radius = config.getDouble("radius", 8.0);
        multiplier = config.getDouble("damage-multiplier", 1.20);
        healPerSecond = config.getDouble("heal-per-second", 20.0);
        duration = config.getLong("duration-ms", 8000L);
        speedAmp = config.getInt("speed-amplifier", 1);

        cost = new CooldownCost(
                cooldowns,
                "rallybanner",
                config.getLong("cooldown-ms", 35000L),
                upgrades
        );
    }

    @Override
    public String getName() {
        return "Rally Banner";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location bannerLocation = findGroundLocation(player);
        BlockDisplay banner = player.getWorld().spawn(
                bannerLocation.clone().add(0.5, 0.0, 0.5),
                BlockDisplay.class,
                display -> {
                    display.setBlock(Material.WHITE_BANNER.createBlockData());
                    display.setViewRange((float) radius + 16.0f);
                }
        );

        applyBuffs(player);

        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                bannerLocation.clone().add(0.5, 1.0, 0.5),
                35,
                1.0,
                1.0,
                1.0,
                0.1
        );
        player.getWorld().playSound(
                bannerLocation,
                Sound.BLOCK_BELL_USE,
                1.0f,
                1.0f
        );

        new BukkitRunnable() {
            private long elapsed;

            @Override
            public void run() {
                if (elapsed >= duration || banner.isDead()) {
                    banner.remove();
                    cancel();
                    return;
                }

                healAllies(player, bannerLocation);
                elapsed += 1000L;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private Location findGroundLocation(Player player) {
        Location playerLocation = player.getLocation();
        int x = playerLocation.getBlockX();
        int z = playerLocation.getBlockZ();
        int startY = playerLocation.getBlockY() - 1;

        for (int y = startY; y >= player.getWorld().getMinHeight(); y--) {
            if (player.getWorld().getBlockAt(x, y, z).getType().isSolid()) {
                return new Location(player.getWorld(), x, y + 1, z);
            }
        }

        return playerLocation.getBlock().getLocation();
    }

    private void applyBuffs(Player player) {
        for (Player ally : player.getWorld().getPlayers()) {
            if (!isNearbyAlly(player, ally)) {
                continue;
            }

            damage.applyBuff(ally, multiplier, duration, "Rally Banner");
            speed.applyBuff(ally, speedAmp, duration);
        }
    }

    private void healAllies(Player player, Location bannerLocation) {
        for (Player ally : player.getWorld().getPlayers()) {
            if (!isNearbyAlly(player, ally)
                    || ally.getLocation().distanceSquared(bannerLocation) > radius * radius) {
                continue;
            }

            health.heal(ally, healPerSecond, getName());
            ally.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    ally.getLocation().add(0.0, 1.0, 0.0),
                    5,
                    0.2,
                    0.4,
                    0.2,
                    0.0
            );
        }
    }

    private boolean isNearbyAlly(Player player, Player other) {
        return !teams.isEnemy(player, other)
                && other.getLocation().distanceSquared(player.getLocation()) <= radius * radius;
    }

    @Override
    public String getDescription() {
        return "Rally nearby allies, granting damage, speed and 20 healing per second.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Radius", radius + " blocks"),
                new AbilityStat("Healing", (int) healPerSecond + " HP/s"),
                new AbilityStat("Duration", (duration / 1000L) + "s")
        );
    }
}
