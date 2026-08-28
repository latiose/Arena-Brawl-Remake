package org.latios.arenaBrawl.abilities;


import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.support.LifeLeechManager;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.SongOfPowerManager;
import org.latios.arenaBrawl.abilities.ultimate.BroodMotherEntityManager;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

public record AbilityDependencies(
        CooldownManager cooldownManager,
        TeamManager teamManager,
        UsageManager usageManager,
        EnergyManager energyManager,
        ShieldManager shieldManager,
        DebuffManager debuffManager,
        PlayerHealthManager playerHealthManager,
        CombatService combatService,
        OrbitShieldManager orbitShieldManager,
        CombatUpgradeManager combatUpgradeManager,
        BroodMotherEntityManager broodMotherEntityManager,
        StructureManager structureManager,
        MovementLockManager movementLockManager,
        SongOfPowerManager songOfPowerManager,
        LifeLeechManager lifeLeechManager,
        StructureDemolitionService demolitionService
) {}