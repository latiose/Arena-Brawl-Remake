package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class Consume implements Ability {

    private final AbilityCost cost;
    private final double damage;
    private final double selfHeal;
    private final double energyCost;
    private final double maxRange;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final PlayerHealthManager healthManager;

    public Consume(EnergyManager energyManager, TeamManager teamManager,
                   CombatService combatService, PlayerHealthManager healthManager, AbilityConfig config) {
        this.damage = config.getDouble("damage", 220.0);
        this.selfHeal = config.getDouble("self-heal", 50.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.maxRange = config.getDouble("max-range", 3.0);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Consume"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Devours the nearest targetted enemy, dealing damage and healing yourself.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Self Heal", String.valueOf((int) selfHeal)),
                new AbilityStat("Range", String.valueOf(maxRange)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        combatService.applyAbilityDamage(player, target, damage, getName());
        healthManager.heal(player, selfHeal, getName());

        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 0.7f);
        return true;
    }
}