package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class NanoBoostAbility implements Ability {

    private static final double MAX_RANGE = 20.0;
    private static final long DURATION_MILLIS = 8_000;
    private static final int SPEED_AMPLIFIER = 1; // Speed II

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final ShieldManager shieldManager;
    private final DamageBuffManager damageBuffManager;
    private final CooldownManager cooldownManager;
    private static final long CHARGE_TIME_MILLIS = 60_000;
    public NanoBoostAbility(CooldownManager cooldownManager, UsageManager usageManager, TeamManager teamManager,
                            ShieldManager shieldManager, DamageBuffManager damageBuffManager) {
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "nanoboost");
        this.teamManager = teamManager;
        this.shieldManager = shieldManager;
        this.damageBuffManager = damageBuffManager;
    }

    @Override
    public String getName() { return "Nano Boost"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "nanoboost", CHARGE_TIME_MILLIS);
    }
    @Override
    public String getDescription() {
        return "Targets an ally, granting them a speed boost,damage reduction, "
                + "and increased damage for some time.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", String.valueOf(MAX_RANGE)),
                new AbilityStat("Duration", "8s"),
                new AbilityStat("Damage Reduction", "50%"),
                new AbilityStat("Damage Increase", "50%"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findAllyAlongRay(player, teamManager, MAX_RANGE);

        if (target == null) {
            player.sendMessage("§eThere is no valid target within range.");
            return false;
        }

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, (int) (DURATION_MILLIS / 50), SPEED_AMPLIFIER, true, false
        ));
        shieldManager.applyShield(target, 0.5, DURATION_MILLIS);
        damageBuffManager.applyBuff(target, 1.5, DURATION_MILLIS, "NANO BOOST");

        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);

        target.sendMessage("§eYou're powered up! Get in there!");
        player.sendMessage("§eYou nano boosted §e" + target.getName() + "§3!");

        startAmbientParticles(target);

        return true;
    }

    private void startAmbientParticles(Player target) {
        long durationTicks = DURATION_MILLIS / 50L;

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
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 3L);
    }
}