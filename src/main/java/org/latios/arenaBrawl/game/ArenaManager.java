package org.latios.arenaBrawl.game;


import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.support.EtherealBodyManager;
import org.latios.arenaBrawl.abilities.support.LifeLeechManager;
import org.latios.arenaBrawl.abilities.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.SongOfPowerManager;
import org.latios.arenaBrawl.abilities.ultimate.BroodMotherEntityManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.hats.HatEquipUtils;
import org.latios.arenaBrawl.hats.HatSelectionManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.powerups.PowerupType;
import org.latios.arenaBrawl.team.Team;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeType;

import java.util.List;

public class ArenaManager {

    private final TeamManager teamManager;
    private final AbilityManager abilityManager;
    private final AbilityRegistry abilityRegistry;
    private final AbilitySelectionManager selectionManager;
    private final CooldownManager cooldownManager;
    private final UsageManager usageManager;
    private final ScoreboardManager scoreboardManager;
    private final org.bukkit.plugin.Plugin plugin;
    private final EnergyManager energyManager;
    private final PlayerHealthManager playerHealthManager;
    private final HungerManager hungerManager;
    private final MatchManager matchManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final ArmorTierManager armorTierManager;
    private final CombatService combatService;
    private final OrbitShieldManager orbitShieldManager;
    private final HatSelectionManager hatSelectionManager;
    private final CombatUpgradeManager combatUpgradeManager;
    private final BroodMotherEntityManager broodMotherEntityManager;
    private final ArenaMapManager arenaMapManager;
    private final StructureManager structureManager;
    private final MovementLockManager movementLockManager;
    private final SongOfPowerManager songOfPowerManager;
    private final LifeLeechManager lifeLeechManager;
    private final StructureDemolitionService demolitionService;
    private final EnergyModifierManager energyModifierManager;
    private final DamageBuffManager damageBuffManager;
    private final EtherealBodyManager etherealBodyManager;
    private final DamageVulnerabilityManager damageVulnerabilityManager;
    private final SpeedBuffManager speedBuffManager;
    public ArenaManager(TeamManager teamManager, AbilityManager abilityManager, AbilityRegistry abilityRegistry,
                        AbilitySelectionManager selectionManager, CooldownManager cooldownManager,
                        UsageManager usageManager, ScoreboardManager scoreboardManager,
                        org.bukkit.plugin.Plugin plugin, EnergyManager energyManager, PlayerHealthManager playerHealthManager, HungerManager hungerManager, MatchManager matchManager,ShieldManager shieldManager,
    DebuffManager debuffManager,ArmorTierManager armorTierManager, CombatService combatService,OrbitShieldManager orbitShieldManager, HatSelectionManager hatSelectionManager,
                        CombatUpgradeManager combatUpgradeManager,BroodMotherEntityManager broodMotherEntityManager, ArenaMapManager arenaMapManager, StructureManager structureManager, MovementLockManager movementLockManager, SongOfPowerManager songOfPowerManager,
                        LifeLeechManager lifeLeechManager, StructureDemolitionService demolitionService,EnergyModifierManager energyModifierManager, DamageBuffManager damageBuffManager,
                        EtherealBodyManager etherealBodyManager, DamageVulnerabilityManager damageVulnerabilityManager,SpeedBuffManager speedBuffManager) {
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
        this.abilityRegistry = abilityRegistry;
        this.selectionManager = selectionManager;
        this.cooldownManager = cooldownManager;
        this.usageManager = usageManager;
        this.scoreboardManager = scoreboardManager;
        this.energyManager = energyManager;
        this.plugin = plugin;
        this.playerHealthManager = playerHealthManager;
        this.hungerManager = hungerManager;
        this.matchManager = matchManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.armorTierManager = armorTierManager;
        this.combatService = combatService;
        this.orbitShieldManager = orbitShieldManager;
        this.hatSelectionManager = hatSelectionManager;
        this.combatUpgradeManager = combatUpgradeManager;
        this.broodMotherEntityManager = broodMotherEntityManager;
        this.arenaMapManager = arenaMapManager;
        this.structureManager = structureManager;
        this.movementLockManager = movementLockManager;
        this.songOfPowerManager = songOfPowerManager;
        this.lifeLeechManager = lifeLeechManager;
        this.demolitionService = demolitionService;
        this.energyModifierManager = energyModifierManager;
        this.damageBuffManager = damageBuffManager;
        this.etherealBodyManager = etherealBodyManager;
        this.damageVulnerabilityManager = damageVulnerabilityManager;
        this.speedBuffManager = speedBuffManager;
    }

    public void startMatch(Player p1, Player p2, Player p3, Player p4) {
        ArenaMap map = arenaMapManager.claimAvailableMap();

        if (map == null || !map.isValid()) {
            if (map != null) {
                arenaMapManager.releaseMap(map);
            }
            for (Player p : List.of(p1, p2, p3, p4)) {
                p.sendMessage("§cAll arenas are currently occupied or invalid. Please wait.");
            }
            return;
        }
        teamManager.setTeam(p1, Team.RED);
        teamManager.setTeam(p2, Team.RED);
        teamManager.setTeam(p3, Team.BLUE);
        teamManager.setTeam(p4, Team.BLUE);

        List<Player> allPlayers = List.of(p1, p2, p3, p4);
        AbilityDependencies deps = new AbilityDependencies(cooldownManager, teamManager, usageManager, energyManager, shieldManager, debuffManager, playerHealthManager, combatService, orbitShieldManager,combatUpgradeManager,broodMotherEntityManager,structureManager,movementLockManager,songOfPowerManager,lifeLeechManager,demolitionService,
                energyModifierManager,damageBuffManager,etherealBodyManager,damageVulnerabilityManager,speedBuffManager);

        for (Player player : allPlayers) {
            player.setCollidable(false);
            playerHealthManager.setMaxHealth(player, combatUpgradeManager.getValue(player, CombatUpgradeType.HEALTH));
            energyManager.setMaxEnergyOverride(player,combatUpgradeManager.getValue(player, CombatUpgradeType.ENERGY));
            usageManager.resetPlayer(player);
            energyManager.reset(player);
            hungerManager.reset(player);

            for (AbilitySlot slot : AbilitySlot.values()) {
                String abilityId = selectionManager.getSelection(player, slot);
                Ability ability = abilityRegistry.create(slot, abilityId, deps);
                abilityManager.setAbility(player, slot, ability);
                ability.onMatchStart(player);
            }

            AbilityKit.giveDefaultKit(player, abilityManager);
            armorTierManager.equipCosmeticArmor(player);
            HatEquipUtils.applyEquippedHat(player, hatSelectionManager);
            player.updateInventory();
        }

        var match = new Match(List.of(p1, p2), List.of(p3, p4), map);

        match.getPowerupManager().configureLocations(PowerupType.HEALTH,
                map.getHealthPowerupLocation() != null ? List.of(map.getHealthPowerupLocation()) : List.of());
        match.getPowerupManager().configureLocations(PowerupType.DOUBLE_DAMAGE, map.getDamagePowerupLocations());
        match.getPowerupManager().reset();

        match.setIndividualScoreboards(scoreboardManager.createIndividualScoreboards(match));
        scoreboardManager.updateHealthDisplay(match);

        CollisionUtils.disableCollisionForGroup(match.getAllPlayers());

        var task = new MatchScoreboardTask(match, scoreboardManager).runTaskTimer(plugin, 0L, 10L);
        matchManager.registerMatch(match, task);

        p1.teleport(map.getRedSpawn1());
        p2.teleport(map.getRedSpawn2());
        p3.teleport(map.getBlueSpawn1());
        p4.teleport(map.getBlueSpawn2());

    }

}