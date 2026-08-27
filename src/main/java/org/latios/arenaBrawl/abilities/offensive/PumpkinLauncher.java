package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Material;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

public class PumpkinLauncher extends AbstractLauncher {

    private static final double DAMAGE = 70.0;
    private static final double SLICE_DAMAGE = 5.0;
    private static final double ENERGY_COST = 30.0;
    private static final double AOE_RADIUS = 3.0;

    public PumpkinLauncher(EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        super(energyManager, ENERGY_COST, teamManager, combatService);
    }

    @Override
    public String getName() { return "Pumpkin Launcher"; }

    @Override
    public String getDescription() {
        return "Launches a pumpkin that splits into 3 pumpkin pies on impact.";
    }

    @Override
    protected double getMainDamage() { return DAMAGE; }

    @Override
    protected double getSliceDamage() { return SLICE_DAMAGE; }

    @Override
    protected double getAoeRadius() { return AOE_RADIUS; }

    @Override
    protected Material getHeadMaterial() { return Material.CARVED_PUMPKIN; }

    @Override
    protected Material getSliceMaterial() { return Material.PUMPKIN_PIE; }
}