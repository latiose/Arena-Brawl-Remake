package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RallyBanner implements Ability {

    private static final String BUFF_TITLE = "Rally Banner";

    /**
     * A banner in progress is per-player state that must survive the ability
     * instance (one per match), so it lives in a static registry like every
     * other timed ability state.
     */
    private record ActiveBanner(Object token, BukkitTask task, ItemStack savedHelmet,
                                Set<UUID> buffedPlayers, DamageBuffManager damage, SpeedBuffManager speed) {
    }

    private static final Map<UUID, ActiveBanner> ACTIVE = new ConcurrentHashMap<>();

    /**
     * Cancels the caster's banner (restoring the helmet) and revokes the buffs
     * it granted. Safe to call on death, match end, /hub or disconnect.
     */
    public static void clear(Player player) {
        ActiveBanner active = ACTIVE.remove(player.getUniqueId());
        if (active == null) return;

        active.task().cancel();
        restoreHelmet(player, active.savedHelmet());

        for (UUID buffedId : active.buffedPlayers()) {
            Player buffed = Bukkit.getPlayer(buffedId);
            if (buffed == null) continue;
            active.damage().removeBuff(buffed, BUFF_TITLE);
            active.speed().removeBuff(buffed, BUFF_TITLE);
        }
    }

    /**
     * Removes the rally buffs a player received as an ally. The caster's own
     * buffs are handled by {@link #clear(Player)}; this covers an ally dying
     * or leaving while the banner is still up.
     */
    public static void clearReceivedBuffs(Player player, DamageBuffManager damage, SpeedBuffManager speed) {
        damage.removeBuff(player, BUFF_TITLE);
        speed.removeBuff(player, BUFF_TITLE);
    }

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
    private final long cooldownMs;

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
        cooldownMs = config.getLong("cooldown-ms", 35000L);

        cost = new CooldownCost(
                cooldowns,
                "rallybanner",
                cooldownMs,
                upgrades
        );
    }

    @Override
    public String getName() {
        return BUFF_TITLE;
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        // Casting again while a banner is up must not stack tasks or helmets.
        clear(player);

        ItemStack previousHelmet = player.getInventory().getHelmet();
        ItemStack savedHelmet = previousHelmet == null ? null : previousHelmet.clone();
        ItemStack banner = createBanner();

        // The banner is worn as a helmet so it follows the caster around.
        player.getInventory().setHelmet(banner.clone());

        Set<UUID> buffedPlayers = applyBuffs(player);
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
        UUID casterId = player.getUniqueId();
        Object token = new Object();

        BukkitRunnable task = new BukkitRunnable() {
            private long ticks;

            @Override
            public void run() {
                // Dying, logging out or being eliminated mid-banner ends it
                // immediately: no leftover buffs and no banner helmet on the
                // spectator.
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR) {
                    clear(player);
                    cancel();
                    return;
                }

                if (ticks >= durationTicks) {
                    // Natural expiry: the buffs expire on their own so the
                    // players still get the "has expired" notice.
                    ActiveBanner active = ACTIVE.get(casterId);
                    if (active != null && active.token() == token) {
                        ACTIVE.remove(casterId);
                        restoreHelmet(player, savedHelmet);
                    }
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
        };

        BukkitTask scheduled = task.runTaskTimer(plugin, 0L, 1L);
        ACTIVE.put(casterId, new ActiveBanner(token, scheduled, savedHelmet, buffedPlayers, damage, speed));

        return true;
    }

    private static ItemStack createBanner() {
        return new ItemStack(Material.WHITE_BANNER);
    }

    private static boolean isRallyBanner(ItemStack item) {
        return item != null && item.getType() == Material.WHITE_BANNER;
    }

    private static void restoreHelmet(Player player, ItemStack previousHelmet) {
        if (!player.isOnline() || !isRallyBanner(player.getInventory().getHelmet())) {
            return;
        }
        player.getInventory().setHelmet(previousHelmet);
        player.updateInventory();
    }

    private Set<UUID> applyBuffs(Player player) {
        Set<UUID> buffed = ConcurrentHashMap.newKeySet();
        for (Player ally : player.getWorld().getPlayers()) {
            if (!isNearbyAlly(player, ally)) {
                continue;
            }

            damage.applyBuff(ally, multiplier, duration, BUFF_TITLE);
            speed.applyBuff(ally, speedAmp, duration, BUFF_TITLE);
            buffed.add(ally.getUniqueId());
        }
        return buffed;
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

    /**
     * Only teammates still fighting count: spectators (eliminated players have
     * their team cleared and/or are in SPECTATOR mode) must never receive the
     * buff or the healing.
     */
    private boolean isNearbyAlly(Player player, Player other) {
        if (!other.isOnline() || other.isDead() || other.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        if (!teams.isAlly(player, other)) {
            return false;
        }
        return other.getLocation().distanceSquared(player.getLocation()) <= radius * radius;
    }

    @Override
    public String getDescription() {
        return "Rallies teammates within " + trim(radius) + " blocks for "
                + (duration / 1000L) + "s: +" + damageBonusPercent() + "% damage, Speed "
                + roman(speedAmp + 1) + ", and " + trim(healPerSecond) + " HP/s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Radius", trim(radius) + " blocks"),
                new AbilityStat("Damage", "+" + damageBonusPercent() + "%"),
                new AbilityStat("Speed", "Speed " + roman(speedAmp + 1)),
                new AbilityStat("Healing", trim(healPerSecond) + " HP/s"),
                new AbilityStat("Duration", (duration / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    private int damageBonusPercent() {
        return (int) Math.round((multiplier - 1.0) * 100.0);
    }

    private static String trim(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value)
                : String.format(java.util.Locale.US, "%.1f", value);
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
