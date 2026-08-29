package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public abstract class AbstractLauncher implements Ability {

    protected final AbilityCost cost;
    protected final TeamManager teamManager;
    protected final CombatService combatService;

    public AbstractLauncher(EnergyManager energyManager, double energyCost, TeamManager teamManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        ArmorStand stand = player.getWorld().spawn(player.getEyeLocation().subtract(0, 1.2, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.getEquipment().setHelmet(new ItemStack(getHeadMaterial()));
        });

        Vector velocity = player.getLocation().getDirection().multiply(1.2);
        velocity.setY(velocity.getY() + 0.2);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1.2f);

        new TrackedLauncherTask(stand, velocity, player, getMainDamage(), getSliceDamage(),
                getAoeRadius(), getName(), getHeadMaterial(), getSliceMaterial(), teamManager, combatService)
                .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Main Damage", String.valueOf((int) getMainDamage())),
                new AbilityStat("Slice Damage", String.valueOf((int) getSliceDamage())),
                new AbilityStat("Explosion Radius", String.valueOf((int) getAoeRadius())),
                new AbilityStat("Slices", "3"),
                new AbilityStat("Energy Cost", cost.getBaseCostDescription())
        );
    }

    protected abstract double getMainDamage();
    protected abstract double getSliceDamage();
    protected abstract double getAoeRadius();
    protected abstract Material getHeadMaterial();
    protected abstract Material getSliceMaterial();
}