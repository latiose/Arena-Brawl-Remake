package org.latios.arenaBrawl.abilities.support;

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
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Individualistic self-sustain. While no ally is nearby, the caster regenerates health and gains a
 * damage bonus. If a teammate gets too close, the effect ends, encouraging solo flanks in 2v2.
 *
 * <p>The damage bonus is applied once through {@link DamageBuffManager} for the full duration, so
 * there is no refresh loop and no repeated "has expired" chat spam.</p>
 */
public class LoneWolf implements Ability {

    private static final long CHECK_INTERVAL_TICKS = 10L;
    private static final long ALLY_GRACE_TICKS = 20L;
    private static final String BUFF_TITLE = "Lone Wolf";

    private final long cooldownMs;
    private final long durationMs;
    private final double healPerSecond;
    private final double allyRadius;
    private final double damageBonus;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final DamageBuffManager damageBuffManager;
    private final AbilityConfig config;

    public LoneWolf(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                    PlayerHealthManager healthManager, DamageBuffManager damageBuffManager,
                    CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 35000L);
        this.durationMs = config.getLong("duration-ms", 8000L);
        this.healPerSecond = config.getDouble("heal-per-second", 25.0);
        this.allyRadius = config.getDouble("ally-radius", 10.0);
        this.damageBonus = config.getDouble("damage-bonus", 0.08);

        this.cost = new CooldownCost(cooldownManager, "lonewolf", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.damageBuffManager = damageBuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Lone Wolf";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "While no ally is within " + (int) allyRadius + " blocks, regenerate health and deal extra damage. "
                + "If an ally gets close, it ends.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Healing", (int) healPerSecond + " HP/s"),
                new AbilityStat("Damage", "+" + (int) (damageBonus * 100) + "%"),
                new AbilityStat("Ally Distance", (int) allyRadius + "m"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        damageBuffManager.applyBuff(player, 1.0 + damageBonus, durationMs, BUFF_TITLE);

        long maxTicks = durationMs / 50L;
        double healPerCheck = healPerSecond * (CHECK_INTERVAL_TICKS / 20.0);

        MatchSoundUtils.play(config, player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.6f);

        new BukkitRunnable() {
            long elapsedTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    damageBuffManager.removeBuff(player, BUFF_TITLE);
                    cancel();
                    return;
                }

                if (elapsedTicks >= maxTicks) {
                    cancel();
                    return;
                }

                if (elapsedTicks >= ALLY_GRACE_TICKS && hasNearbyAlly(player)) {
                    damageBuffManager.removeBuff(player, BUFF_TITLE);
                    player.sendMessage(MessageUtils.negative() + "§3Your Lone Wolf ended because an ally came too close.");
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
                    cancel();
                    return;
                }

                healthManager.heal(player, healPerCheck,getName());

                Location body = player.getLocation().add(0, 1.0, 0);
                body.getWorld().spawnParticle(Particle.HEART, body, 1, 0.3, 0.3, 0.3, 0.0);
                body.getWorld().spawnParticle(Particle.CRIT, body, 2, 0.3, 0.3, 0.3, 0.05);

                elapsedTicks += CHECK_INTERVAL_TICKS;
            }
        }.runTaskTimer(plugin, 0L, CHECK_INTERVAL_TICKS);

        return true;
    }

    private boolean hasNearbyAlly(Player player) {
        double radiusSq = allyRadius * allyRadius;

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || other.isDead() || !other.isOnline()) continue;
            if (!teamManager.isAlly(player, other)) continue;
            if (other.getLocation().distanceSquared(player.getLocation()) <= radiusSq) {
                return true;
            }
        }
        return false;
    }
}
