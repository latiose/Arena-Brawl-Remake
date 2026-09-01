package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Material;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

public class PumpkinLauncher extends AbstractLauncher {

    private static final double DEFAULT_DAMAGE = 70.0;
    private static final double DEFAULT_SLICE_DAMAGE = 5.0;
    private static final double DEFAULT_ENERGY_COST = 30.0;
    private static final double DEFAULT_AOE_RADIUS = 3.0;

    public PumpkinLauncher(EnergyManager energyManager, TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        super(energyManager, teamManager, combatService, config, DEFAULT_DAMAGE, DEFAULT_SLICE_DAMAGE, DEFAULT_ENERGY_COST, DEFAULT_AOE_RADIUS);
    }

    @Override
    public String getName() { return "Pumpkin Launcher"; }

    @Override
    public String getDescription() {
        return "Launches a pumpkin that splits into 3 pumpkin pies on impact.";
    }

    @Override
    protected Material getHeadMaterial() { return Material.CARVED_PUMPKIN; }

    @Override
    protected Material getSliceMaterial() { return Material.PUMPKIN_PIE; }
}