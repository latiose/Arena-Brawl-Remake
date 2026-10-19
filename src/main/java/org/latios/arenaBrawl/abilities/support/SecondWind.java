package org.latios.arenaBrawl.abilities.support;

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
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class SecondWind implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final double baseHeal;
    private final double missingHealthPercent;
    private final double hotTotalHeal;
    private final long hotDurationMs;
    private final int hotIntervalTicks;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public SecondWind(Plugin plugin, CooldownManager cooldownManager, PlayerHealthManager healthManager,
                      CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 35000L);
        this.baseHeal = config.getDouble("base-heal", 250.0);
        this.missingHealthPercent = config.getDouble("missing-health-percent", 0.25);
        this.hotTotalHeal = config.getDouble("hot-total-heal", 200.0);
        this.hotDurationMs = config.getLong("hot-duration-ms", 5000L);
        this.hotIntervalTicks = Math.max(1, config.getInt("hot-interval-ticks", 10));

        this.cost = new CooldownCost(cooldownManager, "secondwind", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Second Wind"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        double current = healthManager.getHealth(player);
        double max = healthManager.getMaxHealth(player);
        double missing = Math.max(0.0, max - current);
        double instant = baseHeal + missing * missingHealthPercent;

        healthManager.heal(player, instant, getName());
        startHealOverTime(player);

        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 12, 0.5, 0.6, 0.5, 0.02);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.7, 0.5, 0.15);

        MatchSoundUtils.play(config, player, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.1f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.4f);

        return true;
    }

    private void startHealOverTime(Player player) {
        if (hotTotalHeal <= 0.0 || hotDurationMs <= 0L) return;

        final int pulses = Math.max(1, (int) (hotDurationMs / (hotIntervalTicks * 50L)));
        final double healPerPulse = hotTotalHeal / pulses;

        new BukkitRunnable() {
            int done = 0;

            @Override
            public void run() {
                if (done >= pulses || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }
                healthManager.heal(player, healPerPulse, getName());
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1, 0.3, 0.1, 0.3, 0.0);
                done++;
            }
        }.runTaskTimer(plugin, hotIntervalTicks, hotIntervalTicks);
    }

    @Override
    public String getDescription() {
        return "Draw on your resilience for an instant burst of health that grows the more health you are missing, then keep regenerating over a few seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Base Heal", String.valueOf((int) baseHeal)),
                new AbilityStat("Missing HP Bonus", "up to +" + (int) (missingHealthPercent * 100) + "%"),
                new AbilityStat("Heal Over Time", (int) hotTotalHeal + " over " + (hotDurationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
