package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class KingsGuard implements Ability {

    private final double energyCost;
    private final int maxGuards;
    private final double maxRange;
    private final double normalAttackDamage;
    private final double firstChargeDamage;
    private final double repeatChargeDamage;
    private final int hitsToDestroy;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final KingsGuardEntityManager entityManager;

    public KingsGuard(EnergyManager energyManager, TeamManager teamManager,
                      KingsGuardEntityManager entityManager, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 30.0);
        this.maxGuards = config.getInt("max-guards", 3);
        this.maxRange = config.getDouble("max-range", 30.0);
        this.normalAttackDamage = config.getDouble("normal-attack-damage", 50.0);
        this.firstChargeDamage = config.getDouble("first-charge-damage", 100.0);
        this.repeatChargeDamage = config.getDouble("repeat-charge-damage", 50.0);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 3);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.entityManager = entityManager;
    }

    @Override
    public String getName() {
        return "King's Guard";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Summons a golden guard wielding a spear. Guards fight nearby enemies on their own, dealing "
                + (int) normalAttackDamage + " damage per hit. Order them to charge at a targeted enemy: their "
                + "first charge deals " + (int) firstChargeDamage + " damage and later charges "
                + (int) repeatChargeDamage + ".";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Guards", String.valueOf(maxGuards)),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Attack Damage", (int) normalAttackDamage + ""),
                new AbilityStat("Charge Damage", (int) firstChargeDamage + " / " + (int) repeatChargeDamage),
                new AbilityStat("Hits to Destroy", String.valueOf(hitsToDestroy))
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target != null && entityManager.getGuardCount(player) > 0) {
            int ordered = entityManager.orderCharge(player, target);

            if (ordered > 0) {
                player.sendMessage(MessageUtils.positive() + "§eYour King's Guards charge at §e"
                        + target.getName() + "§e!");
                return true;
            }
        }


        if (!entityManager.summonGuard(player)) {
            player.sendMessage(MessageUtils.negative() + "§eYou already have §e" + maxGuards
                    + "§e King's Guards!");
            return false;
        }

        return true;
    }
}
