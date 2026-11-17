package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
        ItemStack previousHelmet = player.getInventory().getHelmet();
        ItemStack savedHelmet = previousHelmet == null ? null : previousHelmet.clone();
        ItemStack banner = createBanner();

        // The banner is worn as a helmet so it follows the caster around.
        player.getInventory().setHelmet(banner.clone());

        applyBuffs(player);
        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0.0, 2.2, 0.0),
                35,
                1.0,
                1.0,
                1.0,
                0.1
        );
        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_BELL_USE,
                1.0f,
                1.0f
        );

        long durationTicks = Math.max(1L, (duration + 49L) / 50L);

        new BukkitRunnable() {
            private long ticks;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= durationTicks) {
                    restoreHelmet(player, savedHelmet);
                    cancel();
                    return;
                }

                // Animated hats (or another plugin) may replace the helmet while
                // the banner is active; keep it equipped for the whole duration.
                if (!isRallyBanner(player.getInventory().getHelmet())) {
                    player.getInventory().setHelmet(banner.clone());
                }

                if (ticks % 20L == 0L) {
                    healAllies(player);
                    player.getWorld().spawnParticle(
                            Particle.HAPPY_VILLAGER,
                            player.getLocation().add(0.0, 2.2, 0.0),
                            5,
                            0.2,
                            0.4,
                            0.2,
                            0.0
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private ItemStack createBanner() {
        return new ItemStack(Material.WHITE_BANNER);
    }

    private boolean isRallyBanner(ItemStack item) {
        return item != null && item.getType() == Material.WHITE_BANNER;
    }

    private void restoreHelmet(Player player, ItemStack previousHelmet) {
        if (!player.isOnline() || !isRallyBanner(player.getInventory().getHelmet())) {
            return;
        }
        player.getInventory().setHelmet(previousHelmet);
        player.updateInventory();
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

    private void healAllies(Player player) {
        for (Player ally : player.getWorld().getPlayers()) {
            if (!isNearbyAlly(player, ally)) {
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
