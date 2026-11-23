package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class ArcaneSiphon implements Ability {

    private final double energyCost;
    private final double baseDamage;
    private final double maxDrain;
    private final double damagePerEnergy;
    private final double maxRange;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final EnergyManager energyManager;

    public ArcaneSiphon(EnergyManager energyManager, TeamManager teamManager,
                        CombatService combatService, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.baseDamage = config.getDouble("damage", 40.0);
        this.maxDrain = config.getDouble("max-drain", 45.0);
        this.damagePerEnergy = config.getDouble("damage-per-energy", 2.0);
        this.maxRange = config.getDouble("max-range", 18.0);

        this.energyManager = energyManager;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.cost = new EnergyCost(energyManager, energyCost);
    }

    @Override
    public String getName() {
        return "Arcane Siphon";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);
        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        double drained = Math.min(maxDrain, energyManager.getEnergy(target));
        if (drained > 0) {
            player.sendMessage(MessageUtils.noValidPlayer());
            energyManager.removeEnergy(target, drained);
        }

        double damage = baseDamage + drained * damagePerEnergy;

        drawBeam(player.getEyeLocation(), target.getLocation().add(0, 1.0, 0));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.7f);
        target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1.0, 0),
                20, 0.35, 0.6, 0.35, 0.4);


        combatService.applyAbilityDamage(player, target, damage, getName(), target.getLocation());
        return true;
    }

    private void drawBeam(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length <= 0) return;

        direction.normalize().multiply(0.25);
        Location current = start.clone();
        double traveled = 0;

        while (traveled < length) {
            current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 1, 0.02, 0.02, 0.02, 0.0);
            current.add(direction);
            traveled += 0.25;
        }
    }

    @Override
    public String getDescription() {
        return "Fires an arcane beam at an enemy, draining their energy and converting every point of it into extra damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Base Damage", String.valueOf((int) baseDamage)),
                new AbilityStat("Damage Per Energy", String.format("%.1f", damagePerEnergy)),
                new AbilityStat("Max Drain", String.valueOf((int) maxDrain)),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
