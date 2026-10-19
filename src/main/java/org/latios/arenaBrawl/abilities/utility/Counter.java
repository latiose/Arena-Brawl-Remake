package org.latios.arenaBrawl.abilities.utility;

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
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility parry stance. For a short window the next direct hit (melee or ability) is fully
 * negated and its damage is reflected back at the attacker. The reflection amount is resolved
 * inside {@code CombatService} from the damage the hit would have dealt.
 */
public class Counter implements Ability {

    public record CounterState(long expiresAt) {}

    private static final Map<UUID, CounterState> ACTIVE_COUNTERS = new ConcurrentHashMap<>();

    /** Removes and returns the active counter of a player, or {@code null} if none/expired. */
    public static CounterState consumeActive(Player player) {
        CounterState state = ACTIVE_COUNTERS.get(player.getUniqueId());
        if (state == null) return null;

        ACTIVE_COUNTERS.remove(player.getUniqueId());

        if (System.currentTimeMillis() >= state.expiresAt()) return null;
        return state;
    }

    public static void clear(Player player) {
        ACTIVE_COUNTERS.remove(player.getUniqueId());
    }

    private final long cooldownMs;
    private final long windowMs;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final AbilityConfig config;

    public Counter(Plugin plugin, CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                   AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 22000L);
        this.windowMs = config.getLong("window-ms", 1500L);

        this.cost = new CooldownCost(cooldownManager, "counter", cooldownMs, combatUpgradeManager);
        this.config = config;
    }

    @Override
    public String getName() {
        return "Counter";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Raises a parry stance. The next hit taken within the window is negated and its damage "
                + "is reflected back at the attacker.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Window", (windowMs / 1000.0) + "s"),
                new AbilityStat("Reflect", "100% of the damage"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        UUID uuid = player.getUniqueId();
        ACTIVE_COUNTERS.put(uuid, new CounterState(System.currentTimeMillis() + windowMs));

        MatchSoundUtils.play(config, player, Sound.ITEM_SHIELD_BLOCK, 1.2f, 1.4f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.8f);

        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1.0, 0), 6, 0.4, 0.4, 0.4, 0.0);
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1.0, 0), 12, 0.3, 0.5, 0.3, 0.1);

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                CounterState state = ACTIVE_COUNTERS.get(uuid);
                if (state == null || ticksElapsed >= windowMs / 50L || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                Location body = player.getLocation().add(0, 1.0, 0);
                double angle = (ticksElapsed * 30) % 360;
                double rad = Math.toRadians(angle);
                body.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                        body.clone().add(Math.cos(rad) * 0.7, 0, Math.sin(rad) * 0.7), 1, 0, 0, 0, 0);

                ticksElapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }
}
