// abilities/ultimate/BroodMotherAbility.java
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;

public class BroodMother implements Ability {

    private final AbilityCost cost;
    private final BroodMotherEntityManager entityManager;

    public BroodMother(CooldownManager cooldownManager, UsageManager usageManager,
                              BroodMotherEntityManager entityManager) {
        this.cost = new UltimateCost(cooldownManager, usageManager, "broodmother");
        this.entityManager = entityManager;
    }

    @Override
    public String getName() { return "BroodMother"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        entityManager.summonBoss(player);
        return true;
    }
}