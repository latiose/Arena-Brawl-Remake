package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
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
    private final double energyCost;
    private final double damage;
    private final double immoChance;
    private final int maxRange;
    private final long immoDurationTicks;
    private final DebuffManager debuffManager;
    private final CombatService combatService;

    public LightningStrike(TeamManager teamManager, EnergyManager energyManager,
                           CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 70.0);
        this.damage = config.getDouble("damage", 165.0);
        this.maxRange = config.getInt("max-range", 18);
        this.immoChance = config.getDouble("immo-chance", 0.50);
        this.immoDurationTicks = config.getLong("immo-duration-ticks", 2000L);
        this.cost = new EnergyCost(energyManager, energyCost);
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

        combatService.applyAbilityDamage(player, target, damage, getName());

        if (Math.random() < immoChance) {
            debuffManager.tryApply(target, DebuffType.IMMOBILIZE, immoDurationTicks);
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
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", String.valueOf(maxRange)),
                new AbilityStat("Immobilization chance", (int) (immoChance * 100) + "%"),
                new AbilityStat("Immobilization duration", (int) (immoDurationTicks / 1000L) + "s")
        );
    }
}