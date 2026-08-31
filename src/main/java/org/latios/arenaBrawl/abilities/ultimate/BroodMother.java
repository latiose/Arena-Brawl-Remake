
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class BroodMother implements Ability {

    private static final long CHARGE_TIME_MILLIS = 60_000;
    private static final int MAX_RANGE = 30;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final BroodMotherEntityManager entityManager;
    private final TeamManager teamManager;

    public BroodMother(CooldownManager cooldownManager, UsageManager usageManager,
                       BroodMotherEntityManager entityManager, TeamManager teamManager) {
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
        cooldownManager.setCooldown(player, "broodmother", CHARGE_TIME_MILLIS);
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, MAX_RANGE);
        entityManager.summonBoss(player, target);
        return true;
    }

    @Override
    public String getDescription() {
        return "Summons a giant spider that hunts enemies. On death, spawns 4 spiderlings. Poisons on hit.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Brood HP", "7"),
                new AbilityStat("Spiderlings", "4 (3 hits each)"),
                new AbilityStat("Poison", "33 dmg/s for 6s"),
                new AbilityStat("Spiderling Damage", "5")
        );
    }
}