package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Team ultimate. Blows a war horn that empowers the caster and every nearby ally with a damage
 * bonus and speed, making it the first ultimate designed around the 2v2 duo rather than one player.
 */
public class WarHorn implements Ability {

    private final long chargeTimeMillis;
    private final long durationMs;
    private final double damageIncrease;
    private final int speedAmplifier;
    private final double radius;
    private final double healAmount;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final DamageBuffManager damageBuffManager;
    private final SpeedBuffManager speedBuffManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public WarHorn(CooldownManager cooldownManager, UsageManager usageManager, TeamManager teamManager,
                   DamageBuffManager damageBuffManager, SpeedBuffManager speedBuffManager,
                   PlayerHealthManager healthManager, AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.durationMs = config.getLong("duration-ms", 8000L);
        this.damageIncrease = config.getDouble("damage-increase", 1.40);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.radius = config.getDouble("radius", 10.0);
        this.healAmount = config.getDouble("heal-amount", 100.0);

        this.cost = new UltimateCost(cooldownManager, usageManager, "warhorn");
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.damageBuffManager = damageBuffManager;
        this.speedBuffManager = speedBuffManager;
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "War Horn";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "warhorn", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Sounds a war horn, granting you and all nearby allies a damage boost, speed and a burst of healing.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage Increase", "+" + (int) ((damageIncrease - 1.0) * 100) + "%"),
                new AbilityStat("Speed", "Speed " + toRoman(speedAmplifier + 1)),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation();

        applyTo(player, player);
        for (Entity nearby : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(nearby instanceof Player ally)) continue;
            if (ally.equals(player) || ally.isDead()) continue;
            if (!teamManager.isAlly(player, ally)) continue;
            if (ally.getLocation().distanceSquared(center) > radius * radius) continue;

            applyTo(player, ally);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 10.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 10.0f, 1.4f);

        center.getWorld().spawnParticle(Particle.NOTE, center.clone().add(0, 2.0, 0), 30, 0.6, 0.4, 0.6, 1.0);
        center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1.0, 0), 60, 0.6, 0.8, 0.6, 0.2);

        return true;
    }

    private void applyTo(Player caster, Player target) {
        damageBuffManager.applyBuff(target, damageIncrease, durationMs, "War Horn");
        speedBuffManager.applyBuff(target, speedAmplifier, durationMs);

        if (target.equals(caster)) {
            healthManager.heal(caster, healAmount, getName());
        } else {
            healthManager.healAlly(caster, target, healAmount, getName());
        }

        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(0, 1.5, 0), 12, 0.4, 0.4, 0.4, 0.0);
        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1.0, 0), 20, 0.3, 0.6, 0.3, 0.05);
    }

    private String toRoman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }
}
