// abilities/impl/PolymorphAbility.java
package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

public class Polymorph implements Ability {

    private static final long DURATION_MILLIS = 8_000;
    private static final double RANGE = 20.0;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    public Polymorph(CooldownManager cooldownManager, TeamManager teamManager, DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "polymorph", 40000,combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Polymorph"; }

    @Override
    public AbilityCost getCost() { return cost; }



    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, 20);

        if (target == null) {
            player.sendMessage("§eThere is not valid player within range!");
            return false;
        }

        boolean applied = debuffManager.tryApply(target, DebuffType.POLYMORPH, DURATION_MILLIS);

        target.getWorld().spawnParticle(Particle.POOF, target.getLocation(), 25);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 1.0f, 1.0f);
        player.sendMessage("§eYou turned " + target.getName() + " into a sheep!");
        target.sendMessage("§e" + player.getName() + "'s polymorph ability turned you into a sheep!");

        return true;
    }
}