package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;

public record AbilityDependencies(
        CooldownManager cooldownManager,
        TeamManager teamManager,
        UsageManager usageManager,
        EnergyManager energyManager,
        ShieldManager shieldManager,
        DebuffManager debuffManager,
        PlayerHealthManager playerHealthManager,
        CombatService combatService,
        OrbitShieldManager orbitShieldManager
) {}