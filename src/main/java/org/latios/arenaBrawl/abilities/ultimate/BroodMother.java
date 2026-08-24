// abilities/ultimate/BroodMother.java
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.team.TeamManager;

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
}