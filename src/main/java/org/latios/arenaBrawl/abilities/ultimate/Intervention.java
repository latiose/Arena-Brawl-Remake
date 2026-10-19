package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
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
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Team ultimate. Marks a targeted ally so that if they take lethal damage within the window they
 * are revived instead of eliminated. The revival is resolved inside {@code PlayerHealthManager},
 * which queries {@link #consumeSave(Player)}.
 */
public class Intervention implements Ability {

    public record Save(UUID casterId, double reviveHealth, long expiresAt) {}

    private static final Map<UUID, Save> ACTIVE_SAVES = new ConcurrentHashMap<>();

    /** Removes and returns the active save of a player, or {@code null} if none/expired. */
    public static Save consumeSave(Player player) {
        Save save = ACTIVE_SAVES.get(player.getUniqueId());
        if (save == null) return null;

        ACTIVE_SAVES.remove(player.getUniqueId());

        if (System.currentTimeMillis() >= save.expiresAt()) return null;
        return save;
    }

    private final long chargeTimeMillis;
    private final double maxRange;
    private final long durationMs;
    private final double reviveHealth;
    private final long immunityMs;
    private final double guardReduction;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final ShieldManager shieldManager;
    private final AbilityConfig config;

    public Intervention(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                        TeamManager teamManager, ShieldManager shieldManager, AbilityConfig config) {
        this.plugin = plugin;
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.maxRange = config.getDouble("max-range", 20.0);
        this.durationMs = config.getLong("duration-ms", 10000L);
        this.reviveHealth = config.getDouble("revive-health", 500.0);
        this.immunityMs = config.getLong("immunity-ms", 1500L);
        this.guardReduction = config.getDouble("guard-reduction", 0.30);

        this.cost = new UltimateCost(cooldownManager, usageManager, "intervention");
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.shieldManager = shieldManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Intervention";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "intervention", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Marks a targeted ally. If they take lethal damage within " + (durationMs / 1000L)
                + "s, they are revived instead of eliminated.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Revive Health", (int) reviveHealth + " HP"),
                new AbilityStat("Window", (durationMs / 1000L) + "s"),
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Target", "Ally"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findAllyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        Save save = new Save(player.getUniqueId(), reviveHealth, System.currentTimeMillis() + durationMs);
        ACTIVE_SAVES.put(target.getUniqueId(), save);

        if (guardReduction > 0) {
            shieldManager.applyShield(target, guardReduction, durationMs, "Intervention");
        }

        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.6f);
        MatchSoundUtils.play(config, target, Sound.ITEM_TOTEM_USE, 0.6f, 1.6f);

        player.sendMessage(MessageUtils.positive() + "§3You placed §bIntervention§3 on " + target.getName() + "§3!");
        target.sendMessage(MessageUtils.positive() + "§3" + player.getName() + " will intervene if you fall!");

        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.05);

        startWatch(player, target, save);
        return true;
    }

    private void startWatch(Player caster, Player target, Save save) {
        long ticks = durationMs / 50L;

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                if (!target.isOnline() || target.isDead() || ticksElapsed >= ticks) {
                    ACTIVE_SAVES.remove(target.getUniqueId(), save);
                    cancel();
                    return;
                }

                // The save was consumed by PlayerHealthManager: the ally was rescued.
                if (!ACTIVE_SAVES.containsKey(target.getUniqueId())) {
                    if (immunityMs > 0) {
                        shieldManager.applyShield(target, 1.0, immunityMs, "Intervention");
                    }

                    Location loc = target.getLocation().add(0, 1, 0);
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 0.5, 0.8, 0.5, 0.25);
                    loc.getWorld().spawnParticle(Particle.HEART, loc, 25, 0.5, 0.5, 0.5, 0.0);
                    MatchSoundUtils.play(config, target, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.2f);

                    cancel();
                    return;
                }

                Location head = target.getLocation().add(0, 2.3, 0);
                double angle = (ticksElapsed * 18) % 360;
                double rad = Math.toRadians(angle);
                head.getWorld().spawnParticle(Particle.END_ROD,
                        head.clone().add(Math.cos(rad) * 0.6, 0, Math.sin(rad) * 0.6), 1, 0, 0, 0, 0);
                head.getWorld().spawnParticle(Particle.WAX_ON,
                        head.clone().add(-Math.cos(rad) * 0.6, 0, -Math.sin(rad) * 0.6), 1, 0, 0, 0, 0);

                ticksElapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
