package org.latios.arenaBrawl.abilities;

import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;

public record AbilityDependencies(
        CooldownManager cooldownManager,
        org.latios.arenaBrawl.team.TeamManager teamManager,
        org.latios.arenaBrawl.abilities.ultimate.UsageManager usageManager,
        EnergyManager energyManager,
        PlayerHealthManager playerHealthManager
) {}