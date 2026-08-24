// abilities/utility/ShadowStep.java
package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

public class ShadowStep implements Ability {

    private static final int MAX_RANGE = 20; 
    private static final long COOLDOWN_MILLIS = 30_000;
    private static final int POST_SHADOW_SPEED_AMPLIFIER = 2;
    private static final int POST_SHADOW_SPEED_DURATION_TICKS = 40; // 2 seconds

    private final AbilityCost cost;
    private final TeamManager teamManager;

    public ShadowStep(CooldownManager cooldownManager, TeamManager teamManager, CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "shadowstep", COOLDOWN_MILLIS,combatUpgradeManager);
        this.teamManager = teamManager;
    }

    @Override
    public String getName() { return "Shadow Step"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, MAX_RANGE);

        if (target == null) {
            player.sendMessage("§eThere is not valid player within range!");
            return false;
        }

        Location teleportLocation = AbilityTargeting.findBehindLocation(target);

        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.3, 0.5, 0.3);
        player.teleport(teleportLocation);
        player.getWorld().spawnParticle(Particle.SMOKE, teleportLocation, 20, 0.3, 0.5, 0.3);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, POST_SHADOW_SPEED_DURATION_TICKS, POST_SHADOW_SPEED_AMPLIFIER, true, false
        ));

        return true;
    }
}