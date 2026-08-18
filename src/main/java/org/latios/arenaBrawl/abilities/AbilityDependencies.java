package org.latios.arenaBrawl.abilities;

import org.latios.arenaBrawl.abilities.ultimate.UsageManager;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.HealthUtils;
import org.latios.arenaBrawl.team.TeamManager;

public record AbilityDependencies(
        CooldownManager cooldownManager,
        org.latios.arenaBrawl.team.TeamManager teamManager,
        org.latios.arenaBrawl.abilities.ultimate.UsageManager usageManager,
        EnergyManager energyManager,
        HealthUtils healthUtils
) {}