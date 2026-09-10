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

public class ZombieApocalypse implements Ability {

    private final long chargeTimeMillis;
    private final int maxRange;
    private final int zombieCount;
    private final int zombieHitsToKill;
    private final double zombieDamage;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final ZombieEntityManager entityManager;
    private final TeamManager teamManager;

    public ZombieApocalypse(CooldownManager cooldownManager, UsageManager usageManager,
                            ZombieEntityManager entityManager, TeamManager teamManager,
                            AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.maxRange = config.getInt("max-range", 30);
        this.zombieCount = config.getInt("zombie-count", 4);
        this.zombieHitsToKill = config.getInt("zombie-hits-to-kill", 4);
        this.zombieDamage = config.getDouble("zombie-damage", 20.0);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "zombieapocalypse");
        this.entityManager = entityManager;
        this.teamManager = teamManager;
    }

    @Override
    public String getName() { return "Zombie Apocalypse"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "zombieapocalypse", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);
        entityManager.summonZombies(player, target, zombieCount);
        return true;
    }

    @Override
    public String getDescription() {
        return "Summons " + zombieCount + " zombies to hunt down enemies. Each zombie deals " + (int) zombieDamage + " damage per hit.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Zombies", zombieCount + " (" + zombieHitsToKill + " hits each)"),
                new AbilityStat("Zombie Damage", String.valueOf((int) zombieDamage))
        );
    }
}