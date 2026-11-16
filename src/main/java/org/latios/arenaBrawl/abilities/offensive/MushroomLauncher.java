package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Material;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

public class MushroomLauncher extends AbstractLauncher {

    private static final double DEFAULT_DAMAGE = 240.0;
    private static final double DEFAULT_SLICE_DAMAGE = 12.0;
    private static final double DEFAULT_ENERGY_COST = 90.0;
    private static final double DEFAULT_AOE_RADIUS = 3.0;

    public MushroomLauncher(EnergyManager energyManager, TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        super(energyManager, teamManager, combatService, config, DEFAULT_DAMAGE, DEFAULT_SLICE_DAMAGE, DEFAULT_ENERGY_COST, DEFAULT_AOE_RADIUS);
    }

    @Override
    public String getName() { return "Mushroom Launcher"; }

    @Override
    public String getDescription() {
        return "Launches a giant red mushroom that splits into 3 mushrooms on impact.";
    }

    @Override
    protected Material getHeadMaterial() { return Material.RED_MUSHROOM_BLOCK; }

    @Override
    protected Material getSliceMaterial() { return Material.RED_MUSHROOM; }
}
