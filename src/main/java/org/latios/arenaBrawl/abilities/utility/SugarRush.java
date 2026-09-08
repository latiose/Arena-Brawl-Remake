package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class SugarRush implements Ability {

    private final long cooldownMillis;
    private final int speedAmplifier;
    private final int speedDurationTicks;
    private final long slowDurationMs;
    private final SpeedBuffManager speedBuffManager;
    private final AbilityCost cost;
    private final DebuffManager debuffManager;

    public SugarRush(CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                     DebuffManager debuffManager, AbilityConfig config, SpeedBuffManager speedBuffManager) {
        this.cooldownMillis = config.getLong("cooldown-ms", 30_000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 2);
        this.speedDurationTicks = config.getInt("speed-duration-ticks", 80);
        this.slowDurationMs = config.getLong("slow-duration-ms", 3_000L);

        this.cost = new CooldownCost(cooldownManager, "sugarrush", cooldownMillis, combatUpgradeManager);
        this.speedBuffManager = speedBuffManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Sugar rush"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        speedBuffManager.applyBuff(player, speedAmplifier, speedDurationTicks * 50L);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    debuffManager.tryApply(player, DebuffType.SLOW, slowDurationMs);
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.02);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 0.8f);
                }
            }
        }.runTaskLater(ArenaBrawlPlugin.getInstance(), speedDurationTicks);

        return true;
    }

    @Override
    public String getDescription() {
        return "Gives the user speed, then crashes slowing them down";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMillis / 1000L) + "s"),
                new AbilityStat("Speed duration", (speedDurationTicks / 20) + "s"),
                new AbilityStat("Speed Level", String.valueOf(speedAmplifier + 1)),
                new AbilityStat("Slow duration", (slowDurationMs / 1000L) + "s")
        );
    }
}