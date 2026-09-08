package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class NanoBoost implements Ability {

    private final double maxRange;
    private final long durationMillis;
    private final int speedAmplifier;
    private final long chargeTimeMillis;
    private final double damageReduction;
    private final double damageIncrease;
    private final SpeedBuffManager speedBuffManager;
    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final ShieldManager shieldManager;
    private final DamageBuffManager damageBuffManager;
    private final CooldownManager cooldownManager;

    public NanoBoost(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                     TeamManager teamManager, ShieldManager shieldManager,
                     DamageBuffManager damageBuffManager, AbilityConfig config, SpeedBuffManager speedBuffManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.shieldManager = shieldManager;
        this.damageBuffManager = damageBuffManager;

        this.maxRange = config.getDouble("max-range", 20.0);
        this.durationMillis = config.getLong("duration-ms", 8000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.damageReduction = config.getDouble("damage-reduction", 0.50);
        this.damageIncrease = config.getDouble("damage-increase", 1.50);
        this.speedBuffManager = speedBuffManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "nanoboost");
    }

    @Override
    public String getName() { return "Nano Boost"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "nanoboost", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Targets an ally, granting them a speed boost, damage reduction, and increased damage for some time.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", String.valueOf(maxRange)),
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Damage Reduction", (int)(damageReduction * 100) + "%"),
                new AbilityStat("Damage Increase", (int)((damageIncrease - 1.0) * 100) + "%"),
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

        speedBuffManager.applyBuff(target, speedAmplifier, durationMillis);
        shieldManager.applyShield(target, damageReduction, durationMillis,"Nano boost");
        damageBuffManager.applyBuff(target, damageIncrease, durationMillis, "Nano boost");

        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);

        target.sendMessage("§eYou're powered up! Get in there!");
        player.sendMessage("§eYou nano boosted §e" + target.getName() + "§e!");

        startAmbientParticles(target);

        return true;
    }

    private void startAmbientParticles(Player target) {
        long durationTicks = durationMillis / 50L;

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!target.isOnline() || target.isDead() || ticksElapsed >= durationTicks) {
                    cancel();
                    return;
                }

                target.getWorld().spawnParticle(
                        Particle.END_ROD,
                        target.getLocation().add(0, 1.0, 0),
                        3,
                        0.3, 0.5, 0.3,
                        0.02
                );

                target.getWorld().spawnParticle(
                        Particle.WAX_ON,
                        target.getLocation().add(0, 0.8, 0),
                        2,
                        0.2, 0.4, 0.2,
                        0.01
                );

                ticksElapsed += 3;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }
}