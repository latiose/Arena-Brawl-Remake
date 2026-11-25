package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class LightningRush implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final long durationMs;
    private final int speedAmplifier;
    private final double hitRadius;
    private final long stunDurationMs;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final SpeedBuffManager speedBuffManager;

    public LightningRush(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                         DebuffManager debuffManager, SpeedBuffManager speedBuffManager,
                         CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 18000L);
        this.durationMs = config.getLong("duration-ms", 3000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 2);
        this.hitRadius = config.getDouble("hit-radius", 1.6);
        this.stunDurationMs = config.getLong("stun-duration-ms", 1000L);

        this.cost = new CooldownCost(cooldownManager, "lightningrush", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        this.speedBuffManager = speedBuffManager;
    }

    @Override
    public String getName() {
        return "Lightning Rush";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        speedBuffManager.applyBuff(player, speedAmplifier, durationMs, getName());

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1.0, 0),
                25, 0.4, 0.6, 0.4, 0.08);

        long durationTicks = Math.max(1L, durationMs / 50L);

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR || ticksElapsed >= durationTicks) {
                    cancel();
                    return;
                }

                Location center = player.getLocation().add(0, 1.0, 0);
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center,
                        3, 0.25, 0.3, 0.25, 0.03);

                Player victim = findEnemyInRange(player);
                if (victim != null) {
                    stun(player, victim);
                    cancel();
                    return;
                }

                ticksElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Player findEnemyInRange(Player player) {
        double radiusSquared = hitRadius * hitRadius;

        for (Entity entity : player.getNearbyEntities(hitRadius, hitRadius, hitRadius)) {
            if (!(entity instanceof Player candidate)) continue;
            if (candidate.equals(player) || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, candidate)) continue;
            if (candidate.getLocation().distanceSquared(player.getLocation()) > radiusSquared) continue;
            return candidate;
        }

        return null;
    }

    private void stun(Player caster, Player victim) {
        debuffManager.tryApply(caster, victim, DebuffType.STUN, stunDurationMs);

        victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, victim.getLocation().add(0, 1.0, 0),
                30, 0.35, 0.6, 0.35, 0.12);
        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.0, 0),
                15, 0.3, 0.5, 0.3, 0.15);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.4f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);
    }

    @Override
    public String getDescription() {
        return "Grants Speed " + roman(speedAmplifier + 1) + " for " + (durationMs / 1000L)
                + "s. The first enemy you collide with is stunned for " + (stunDurationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Speed", "Speed " + roman(speedAmplifier + 1)),
                new AbilityStat("Speed Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Stun", (stunDurationMs / 1000L) + "s"),
                new AbilityStat("Collision Radius", String.format("%.1f blocks", hitRadius)),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
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
