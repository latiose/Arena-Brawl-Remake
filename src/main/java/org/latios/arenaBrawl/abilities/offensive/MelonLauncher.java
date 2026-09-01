package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Material;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

public class MelonLauncher extends AbstractLauncher {

    private static final double DEFAULT_DAMAGE = 140.0;
    private static final double DEFAULT_SLICE_DAMAGE = 10.0;
    private static final double DEFAULT_ENERGY_COST = 60.0;
    private static final double DEFAULT_AOE_RADIUS = 3.0;

    public MelonLauncher(EnergyManager energyManager, TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        super(energyManager, teamManager, combatService, config, DEFAULT_DAMAGE, DEFAULT_SLICE_DAMAGE, DEFAULT_ENERGY_COST, DEFAULT_AOE_RADIUS);
    }

    @Override
    public String getName() { return "Melon Launcher"; }

    @Override
    public String getDescription() {
        return "Launches a melon that splits into 3 slices on impact.";
    }

    @Override
    protected Material getHeadMaterial() { return Material.MELON; }

    @Override
    protected Material getSliceMaterial() { return Material.MELON_SLICE; }
}