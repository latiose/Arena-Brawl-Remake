
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
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, RANGE);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        debuffManager.tryApply(player, target, DebuffType.POLYMORPH, DURATION_MILLIS);

        target.getWorld().spawnParticle(Particle.POOF, target.getLocation(), 25);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 1.0f, 1.0f);
        player.sendMessage("§eYour polymorph ability turned " + target.getName() + " into a sheep!");
        target.sendMessage("§e" + player.getName() + "'s polymorph ability turned you into a sheep!");

        return true;
    }

    @Override
    public String getDescription() {
        return "Morphs targeted enemy into a sheep. Morphed players will heal every second, won't be able to use their abilities or move but they will" +
                "break out if they manage to hit an enemy player with a melee attack.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (DURATION_MILLIS / 1000) + "s"),
                new AbilityStat("Range", RANGE + " blocks"),
                new AbilityStat("Duration", String.valueOf(DURATION_MILLIS/1000)),
                new AbilityStat("Heal per second", "25")
        );
    }
}