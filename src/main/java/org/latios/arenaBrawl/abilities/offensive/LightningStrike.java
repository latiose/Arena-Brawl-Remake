package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

public class LightningStrike implements Ability {

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private static final double ENERGY_COST = 80.0;
    private static final double DAMAGE = 165;
    private final DebuffManager debuffManager;
    private final CombatService combatService;
    private final int maxRange = 18;
    private static final long IMMO_DURATION = 2_000;

    public LightningStrike(TeamManager teamManager, EnergyManager energyManager, CombatService combatService, DebuffManager debuffManager) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Lightning Strike"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target == null || !teamManager.isEnemy(player, target)) {
            player.sendMessage("§cNo player within range!");
            return false;
        }

        combatService.applyAbilityDamage(player, target, DAMAGE, getName());

        if (Math.random() < 0.50) {
            debuffManager.tryApply(target, DebuffType.IMMOBILIZE, IMMO_DURATION);
        }

        target.getWorld().strikeLightningEffect(target.getLocation());
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        return true;
    }
}