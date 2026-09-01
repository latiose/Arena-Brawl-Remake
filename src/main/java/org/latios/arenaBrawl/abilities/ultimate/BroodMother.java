package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;

import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class BroodMother implements Ability {

    private final long chargeTimeMillis;
    private final int maxRange;
    private final int broodHp;
    private final int spiderlingCount;
    private final int spiderlingHitsToKill;
    private final double poisonDamagePerSec;
    private final int poisonDurationSec;
    private final double spiderlingDamage;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final BroodMotherEntityManager entityManager;
    private final TeamManager teamManager;

    public BroodMother(CooldownManager cooldownManager, UsageManager usageManager,
                       BroodMotherEntityManager entityManager, TeamManager teamManager,
                       AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.maxRange = config.getInt("max-range", 30);
        this.broodHp = config.getInt("brood-hp", 7);
        this.spiderlingCount = config.getInt("spiderling-count", 4);
        this.spiderlingHitsToKill = config.getInt("spiderling-hits-to-kill", 3);
        this.poisonDamagePerSec = config.getDouble("poison-damage-per-sec", 33.0);
        this.poisonDurationSec = config.getInt("poison-duration-sec", 6);
        this.spiderlingDamage = config.getDouble("spiderling-damage", 5.0);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "broodmother");
        this.entityManager = entityManager;
        this.teamManager = teamManager;
    }

    @Override
    public String getName() { return "BroodMother"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "broodmother", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);
        entityManager.summonBoss(player, target);
        return true;
    }

    @Override
    public String getDescription() {
        return "Summons a giant spider that hunts enemies. On death, spawns " + spiderlingCount + " spiderlings. Poisons on hit.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Brood HP", String.valueOf(broodHp)),
                new AbilityStat("Spiderlings", spiderlingCount + " (" + spiderlingHitsToKill + " hits each)"),
                new AbilityStat("Poison", (int) poisonDamagePerSec + " dmg/s for " + poisonDurationSec + "s"),
                new AbilityStat("Spiderling Damage", String.valueOf((int) spiderlingDamage))
        );
    }
}