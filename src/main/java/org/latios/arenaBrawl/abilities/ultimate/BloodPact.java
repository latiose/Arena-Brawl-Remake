package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Team ritual: both members of the caster's team pool their health so
 * each ends up with the average of the two, and both gain a damage
 * buff for a short duration. The healthy member donates blood, the
 * wounded one receives it.
 */
public class BloodPact implements Ability {

    private final long chargeTimeMillis;
    private final double damageMultiplier;
    private final long durationMillis;
    private final double healthReductionPercent;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final PlayerHealthManager playerHealthManager;
    private final DamageBuffManager damageBuffManager;

    public BloodPact(CooldownManager cooldownManager, UsageManager usageManager,
                     TeamManager teamManager, PlayerHealthManager playerHealthManager,
                     DamageBuffManager damageBuffManager, AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.damageMultiplier = config.getDouble("damage-multiplier", 1.25);
        this.durationMillis = config.getLong("duration-ms", 6000L);
        this.healthReductionPercent = Math.max(0.0, Math.min(100.0,
                config.getDouble("health-reduction-percent", 10.0)));

        this.cost = new UltimateCost(cooldownManager, usageManager, "bloodpact");
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.playerHealthManager = playerHealthManager;
        this.damageBuffManager = damageBuffManager;
    }

    @Override
    public String getName() {
        return "Blood Pact";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "bloodpact", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Blood ritual: you and your ally pool your health so both end up at the average minus "
                + trimPercent(healthReductionPercent) + "%, then both deal "
                + trimPercent((damageMultiplier - 1.0) * 100.0)
                + "% more damage for " + (durationMillis / 1000.0) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Effect", "Share health (-" + trimPercent(healthReductionPercent) + "%) + damage buff"),
                new AbilityStat("Damage Bonus", "+" + trimPercent((damageMultiplier - 1.0) * 100.0) + "%"),
                new AbilityStat("Duration", (durationMillis / 1000.0) + "s"),
                new AbilityStat("Charge", (chargeTimeMillis / 1000L) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player ally = findAlly(player);

        if (ally != null) {
            double casterHealth = playerHealthManager.getHealth(player);
            double allyHealth = playerHealthManager.getHealth(ally);
            double average = (casterHealth + allyHealth) / 2.0;
            // The ritual consumes a percentage of the pooled health.
            double targetHealth = average * (1.0 - healthReductionPercent / 100.0);

            Location ritualCenter = player.getLocation().clone().add(0, 1, 0);
            ritualCenter.getWorld().playSound(ritualCenter, Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 0.7f);
            ritualCenter.getWorld().playSound(ritualCenter, Sound.ENTITY_WARDEN_HEARTBEAT, 2.0f, 1.0f);

            spawnRitualCircle(ritualCenter);

            for (Player member : List.of(player, ally)) {
                double current = playerHealthManager.getHealth(member);
                double delta = targetHealth - current;

                if (delta > 0) {
                    playerHealthManager.heal(member, delta, getName());
                    member.sendMessage("§eYour §4" + getName() + " §erestores §c+" + (int) Math.round(delta)
                            + " §eHP, leaving you at §c" + (int) Math.round(targetHealth) + " §eHP!");
                } else if (delta < 0) {
                    playerHealthManager.damageSilent(member, -delta, null);
                    member.sendMessage("§eYour §4" + getName() + " §econsumes §c" + (int) Math.round(-delta)
                            + " §eHP, leaving you at §c" + (int) Math.round(targetHealth) + " §eHP!");
                } else {
                    member.sendMessage("§eYour §4" + getName() + " §eleaves your health at §c"
                            + (int) Math.round(targetHealth) + " §eHP!");
                }

                damageBuffManager.applyBuff(member, damageMultiplier, durationMillis, getName());
                member.playSound(member.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.2f);
                member.getWorld().spawnParticle(Particle.DUST,
                        member.getLocation().clone().add(0, 1, 0), 30, 0.3, 0.7, 0.3, 0.02,
                        new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.5f));
            }

            ally.sendMessage("§e§l" + player.getName() + " §eperformed the blood pact! You now both have "
                    + (int) Math.round(targetHealth) + " HP!");
        } else {
            // No ally present: the pact only empowers the caster.
            player.sendMessage("§eNo ally present - the" + getName() + " §estrengthens only you!");
            damageBuffManager.applyBuff(player, damageMultiplier, durationMillis, getName());
            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.DUST,
                    player.getLocation().clone().add(0, 1, 0), 30, 0.3, 0.7, 0.3, 0.02,
                    new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.5f));
        }

        return true;
    }

    private Player findAlly(Player player) {
        for (Player candidate : player.getWorld().getPlayers()) {
            if (candidate.equals(player) || !candidate.isOnline() || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isAlly(player, candidate)) continue;
            return candidate;
        }
        return null;
    }

    private void spawnRitualCircle(Location center) {
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2 * i) / 24;
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            center.getWorld().spawnParticle(Particle.DUST,
                    center.clone().add(x, 0, z), 3, 0.05, 0.05, 0.05, 0.0,
                    new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.3f));
        }
    }

    private static String trimPercent(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value)
                : String.format(java.util.Locale.US, "%.0f", value);
    }
}
