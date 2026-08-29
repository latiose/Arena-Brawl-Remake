
package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class ConsumeAbility implements Ability {

    private static final double DAMAGE = 220.0;
    private static final double SELF_HEAL = 50.0;
    private static final double ENERGY_COST = 100.0;
    private static final double MAX_RANGE = 3.0;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final PlayerHealthManager healthManager;

    public ConsumeAbility(EnergyManager energyManager, TeamManager teamManager,
                          CombatService combatService, PlayerHealthManager healthManager) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
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
        return "Devours the nearest enemy within 3 blocks, dealing 220 damage and healing yourself for 50 HP.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", "220"),
                new AbilityStat("Self Heal", "50 HP"),
                new AbilityStat("Range", "3 blocks"),
                new AbilityStat("Energy Cost", "100")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, MAX_RANGE);

        if (target == null) {
            player.sendMessage("§cNo enemy within range to consume.");
            return false;
        }

        combatService.applyAbilityDamage(player, target, DAMAGE, getName());
        healthManager.heal(player, SELF_HEAL);

        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 0.7f);
        player.sendMessage(MessageUtils.positive()+String.format("§3Your Consume healed you for §a%d §3health!", (int) SELF_HEAL));
        return true;
    }
}