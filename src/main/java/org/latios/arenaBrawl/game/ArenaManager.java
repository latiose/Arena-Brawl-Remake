package org.latios.arenaBrawl.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.ultimate.BroodMotherEntityManager;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.hats.HatEquipUtils;
import org.latios.arenaBrawl.hats.HatSelectionManager;
import org.latios.arenaBrawl.powerups.PowerupType;
import org.latios.arenaBrawl.team.Team;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeType;

import java.util.List;

public class ArenaManager {

    private final ArenaLocation arenaLocation;
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
    public ArenaManager(TeamManager teamManager, AbilityManager abilityManager, AbilityRegistry abilityRegistry,
                        AbilitySelectionManager selectionManager, CooldownManager cooldownManager,
                        UsageManager usageManager, ScoreboardManager scoreboardManager,
                        org.bukkit.plugin.Plugin plugin, EnergyManager energyManager, PlayerHealthManager playerHealthManager, HungerManager hungerManager, MatchManager matchManager,ArenaLocation arenaLocation,ShieldManager shieldManager,
    DebuffManager debuffManager,ArmorTierManager armorTierManager, CombatService combatService,OrbitShieldManager orbitShieldManager, HatSelectionManager hatSelectionManager,
                        CombatUpgradeManager combatUpgradeManager,BroodMotherEntityManager broodMotherEntityManager) {
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
        this.arenaLocation = arenaLocation;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.armorTierManager = armorTierManager;
        this.combatService = combatService;
        this.orbitShieldManager = orbitShieldManager;
        this.hatSelectionManager = hatSelectionManager;
        this.combatUpgradeManager = combatUpgradeManager;
        this.broodMotherEntityManager = broodMotherEntityManager;
    }

    public void startMatch(Player p1, Player p2, Player p3, Player p4) {
        teamManager.setTeam(p1, Team.RED);
        teamManager.setTeam(p2, Team.RED);
        teamManager.setTeam(p3, Team.BLUE);
        teamManager.setTeam(p4, Team.BLUE);

        List<Player> allPlayers = List.of(p1, p2, p3, p4);

        if (arenaLocation.getArenaWorld() == null) {
            for (Player player : allPlayers) {
                player.sendMessage("§cError: cannot load arena");
            }
            return;
        }

        p1.teleport(arenaLocation.redSpawn1());
        p2.teleport(arenaLocation.redSpawn2());
        p3.teleport(arenaLocation.blueSpawn1());
        p4.teleport(arenaLocation.blueSpawn2());

        AbilityDependencies deps = new AbilityDependencies(cooldownManager, teamManager, usageManager, energyManager, shieldManager, debuffManager, playerHealthManager, combatService, orbitShieldManager,combatUpgradeManager,broodMotherEntityManager);

        for (Player player : allPlayers) {
            player.setCollidable(false);
            CollisionUtils.disableCollision(player);
            double maxHealth = combatUpgradeManager.getValue(player, CombatUpgradeType.HEALTH);
            double maxEnergy = combatUpgradeManager.getValue(player, CombatUpgradeType.ENERGY);

            playerHealthManager.setMaxHealth(player, maxHealth);
            energyManager.setMaxEnergyOverride(player, maxEnergy);
            //playerHealthManager.setMaxHealth(player, MATCH_MAX_HEALTH);
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

        var match = new Match(List.of(p1, p2), List.of(p3, p4));

        Location healthLoc = arenaLocation.getHealthPowerupLocation();
        if (healthLoc != null) {
            match.getPowerupManager().configureLocations(PowerupType.HEALTH, List.of(healthLoc));
        } else {
            plugin.getLogger().warning("Health powerup location is not configured — health powerups will not spawn.");
        }

        List<Location> damageLocs = arenaLocation.getDamagePowerupLocations();
        if (!damageLocs.isEmpty()) {
            match.getPowerupManager().configureLocations(PowerupType.DOUBLE_DAMAGE, damageLocs);
        } else {
            plugin.getLogger().warning("Damage powerup locations are not configured — damage powerups will not spawn.");
        }

        match.getPowerupManager().reset();
        match.setIndividualScoreboards(scoreboardManager.createIndividualScoreboards(match));
        scoreboardManager.updateHealthDisplay(match);

        CollisionUtils.disableCollisionForGroup(match.getAllPlayers());
        var task = new MatchScoreboardTask(match, scoreboardManager).runTaskTimer(plugin, 0L, 10L);
        matchManager.registerMatch(match, task);


        new MatchScoreboardTask(match, scoreboardManager).runTaskTimer(plugin, 0L, 10L);

        if (arenaLocation.getArenaWorld() == null) {
            for (Player player : allPlayers) {
                player.sendMessage("§cError: cannot load arena");
            }
        }


        p1.teleport(arenaLocation.redSpawn1());
        p2.teleport(arenaLocation.redSpawn2());
        p3.teleport(arenaLocation.blueSpawn1());
        p4.teleport(arenaLocation.blueSpawn2());


    }
}