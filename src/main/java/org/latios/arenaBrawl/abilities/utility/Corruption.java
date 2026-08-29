package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Corruption implements Ability {

    private static final int MAX_RANGE = 20;
    private static final long DURATION_MILLIS = 5_000;
    private static final long COOLDOWN_MILLIS = 35_000;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    public Corruption(CooldownManager cooldownManager, TeamManager teamManager,
                      CombatUpgradeManager combatUpgradeManager, DebuffManager debuffManager) {
        this.cost = new CooldownCost(cooldownManager, "corruption", COOLDOWN_MILLIS, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Corruption"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, MAX_RANGE);

        if (target == null) {
            player.sendMessage("§eThere is no valid player within range!");
            return false;
        }

        debuffManager.tryApply(player, target, DebuffType.ANTIHEAL, DURATION_MILLIS);

        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 15);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 2.0f);

        player.sendMessage(MessageUtils.positive() + "§3Your Corruption hit §3" + target.getName() + " §3!");
        target.sendMessage(MessageUtils.negative() + "§3You were hit by §a" + player.getName() + "§3's Corruption! Cannot heal for 5s.");

        return true;
    }

    @Override
    public String getDescription() {
        return "Prevents an enemy from healing for 5 seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (COOLDOWN_MILLIS / 1000) + "s"),
                new AbilityStat("Range", MAX_RANGE + " blocks"),
                new AbilityStat("Duration", "5s")
        );
    }
}