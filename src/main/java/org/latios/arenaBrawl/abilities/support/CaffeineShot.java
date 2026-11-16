package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Instantly heals an ally, cleanses their movement debuffs (slow /
 * immobilization) and grants them a short speed boost. The classic low
 * cooldown "peel" support ability.
 */
public class CaffeineShot implements Ability {

    private final double healAmount;
    private final int speedAmplifier;
    private final long speedDurationMs;
    private final long cooldownMs;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager playerHealthManager;
    private final DebuffManager debuffManager;
    private final SpeedBuffManager speedBuffManager;

    public CaffeineShot(CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                        TeamManager teamManager, PlayerHealthManager playerHealthManager,
                        DebuffManager debuffManager, SpeedBuffManager speedBuffManager, AbilityConfig config) {
        this.healAmount = config.getDouble("heal-amount", 150.0);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.speedDurationMs = config.getLong("speed-duration-ms", 3000L);
        this.cooldownMs = config.getLong("cooldown-ms", 15000L);

        this.cost = new CooldownCost(cooldownManager, "caffeineshot", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.playerHealthManager = playerHealthManager;
        this.debuffManager = debuffManager;
        this.speedBuffManager = speedBuffManager;
    }

    @Override
    public String getName() {
        return "Caffeine Shot";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Injects an ally (or yourself) with a " + trim(healAmount) + " HP shot that "
                + "cleanses their debffus and grants Speed "
                + roman(speedAmplifier + 1) + " for " + (speedDurationMs / 1000.0) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Healing", "+" + trim(healAmount)),
                new AbilityStat("Cleanses", "Slow / Immobilize"),
                new AbilityStat("Speed Level", roman(speedAmplifier + 1)),
                new AbilityStat("Speed Duration", (speedDurationMs / 1000.0) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findAllyAlongRay(player, teamManager, 30);
        if (target == null) {
            target = player; // No ally in sight: inject yourself.
        }

        playerHealthManager.healAlly(player, target, healAmount, getName());
        debuffManager.clear(target);
        speedBuffManager.applyBuff(target, speedAmplifier, speedDurationMs, "caffeine_shot");

        Location loc = target.getLocation();
        target.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.4f);
        target.getWorld().playSound(loc, Sound.ENTITY_RABBIT_JUMP, 1.0f, 1.2f);
        target.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1.6, 0), 6, 0.3, 0.2, 0.3, 0.0);
        target.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.6, 0), 15, 0.25, 0.3, 0.25, 0.03);

        return true;
    }

    private static String trim(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value)
                : String.format(java.util.Locale.US, "%.1f", value);
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
