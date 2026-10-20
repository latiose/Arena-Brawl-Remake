package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;

import java.util.List;

/**
 * Summons a high-health iron golem that hunts enemies, absorbs hits and taunts
 * nearby enemies toward itself.
 */
public class GuardianColossus implements Ability {

    private final long chargeTimeMillis;
    private final long durationMs;
    private final int hitsToKill;
    private final int maxHealth;
    private final double attackDamage;
    private final double tauntRadius;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final GuardianColossusEntityManager entityManager;

    public GuardianColossus(CooldownManager cooldownManager, UsageManager usageManager,
                            GuardianColossusEntityManager entityManager, AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.durationMs = config.getLong("duration-ms", 10000L);
        this.hitsToKill = config.getInt("hits-to-kill", 30);
        this.maxHealth = config.getInt("max-health", 3000);
        this.attackDamage = config.getDouble("attack-damage", 60.0);
        this.tauntRadius = config.getDouble("taunt-radius", 8.0);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "guardiancolossus");
        this.entityManager = entityManager;
    }

    @Override
    public String getName() {
        return "Guardian Colossus";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "guardiancolossus", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        entityManager.summonColossus(player);
        return true;
    }

    @Override
    public String getDescription() {
        return "Summons a giant guardian that fights for you for " + (durationMs / 1000L) +
                "s. It absorbs " + hitsToKill + " hits and taunts nearby enemies.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Health", maxHealth + " HP"),
                new AbilityStat("Hits to Kill", String.valueOf(hitsToKill)),
                new AbilityStat("Attack", String.valueOf((int) attackDamage)),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Taunt Radius", String.format("%.1f blocks", tauntRadius))
        );
    }
}
