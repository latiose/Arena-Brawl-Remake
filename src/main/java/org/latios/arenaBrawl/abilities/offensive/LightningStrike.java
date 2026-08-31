package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class LightningStrike implements Ability {

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private static final double ENERGY_COST = 70.0;
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
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        combatService.applyAbilityDamage(player, target, DAMAGE, getName());

        if (Math.random() < 0.50) {
            debuffManager.tryApply(target, DebuffType.IMMOBILIZE, IMMO_DURATION);
        }

        target.getWorld().strikeLightningEffect(target.getLocation());
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.1f, 1.0f);

        return true;
    }

    @Override
    public String getDescription() {
        return "Calls lightning on an enemy, dealing damage and having a chance to immobilize";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + ""),
                new AbilityStat("Range", maxRange+""),
                new AbilityStat("Immobilization chance",  "50%"),
                new AbilityStat("Immobilization duration", (int) IMMO_DURATION/1000 + "")
        );
    }
}