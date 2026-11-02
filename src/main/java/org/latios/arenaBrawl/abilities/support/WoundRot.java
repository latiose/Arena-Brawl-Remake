package org.latios.arenaBrawl.abilities.support;

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
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Marks an enemy so that any healing they receive while the mark is active
 * damages them by the healed amount instead. Rot damage is applied directly
 * through {@link PlayerHealthManager#damage}, bypassing shields like poison.
 */
public class WoundRot implements Ability {

    private final double range;
    private final long durationMs;
    private final long cooldownMs;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager playerHealthManager;
    private final AbilityConfig config;

    public WoundRot(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                    PlayerHealthManager playerHealthManager, CombatUpgradeManager combatUpgradeManager,
                    AbilityConfig config) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.playerHealthManager = playerHealthManager;

        this.range = config.getDouble("range", 15.0);
        this.durationMs = config.getLong("duration-ms", 6000L);
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);

        this.cost = new CooldownCost(cooldownManager, "woundrot", cooldownMs, combatUpgradeManager);
        this.config = config;
    }

    @Override
    public String getName() {
        return "Wound Rot";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Marks the enemy you are looking at: for " + (durationMs / 1000L)
                + "s, every heal they receive turns into the same amount of damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Effect", "Heals damage instead"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Range", (int) range + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, range);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }
        target.sendMessage(MessageUtils.negative() + "§3You are rotted! Any healing you receive will damage you instead!");
        playerHealthManager.markRot(target, player, durationMs);
        long myExpiry = playerHealthManager.getRotMarkExpiry(target);

        Location loc = target.getLocation();
        target.getWorld().playSound(loc, Sound.BLOCK_SCULK_SPREAD, 2.0f, 0.6f);
        target.getWorld().spawnParticle(Particle.ASH, loc.clone().add(0, 1, 0), 50, 0.4, 0.8, 0.4, 0.02);
        target.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 30, 0.3, 0.7, 0.3, 0.02,
                new Particle.DustOptions(Color.fromRGB(40, 80, 40), 1.4f));



        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isOnline()) return;

                Long expiry = playerHealthManager.getRotMarkExpiry(target);
                if (expiry != null && expiry != myExpiry) return;

                playerHealthManager.clearRotMark(target);
                target.sendMessage(MessageUtils.positive() + "§3The rot in your wounds has burned out!");
                target.getWorld().spawnParticle(Particle.COMPOSTER,
                        target.getLocation().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.0);
            }
        }.runTaskLater(plugin, durationMs / 50L);

        return true;
    }
}
