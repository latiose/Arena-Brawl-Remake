package org.latios.arenaBrawl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.EnergyRegenTask;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;
import org.latios.arenaBrawl.game.ArenaManager;
import org.latios.arenaBrawl.game.MatchDisconnectListener;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.gui.AbilityMenuCommand;
import org.latios.arenaBrawl.gui.AbilitySelectorGUI;
import org.latios.arenaBrawl.gui.AbilitySelectorListener;
import org.latios.arenaBrawl.party.PartyCommand;
import org.latios.arenaBrawl.party.PartyManager;
import org.latios.arenaBrawl.queue.QueueCommand;
import org.latios.arenaBrawl.queue.QueueManager;
import org.latios.arenaBrawl.team.TeamManager;

public final class ArenaBrawlPlugin extends JavaPlugin implements Listener {

    private static ArenaBrawlPlugin instance;

    private CooldownManager cooldownManager;
    private AbilityManager abilityManager;
    private TeamManager teamManager;
    private UsageManager usageManager;
    private AbilityRegistry abilityRegistry;
    private AbilitySelectionManager abilitySelectionManager;
    private ScoreboardManager scoreboardManager;
    private ArenaManager arenaManager;
    private EnergyManager energyManager;
    private PlayerHealthManager playerHealthManager;
    private HungerManager hungerManager;
    private PartyManager partyManager;
    private QueueManager queueManager;
    private AbilitySelectorGUI abilitySelectorGUI;
    private AbilityPersistenceManager abilityPersistenceManager;
    private MatchManager matchManager;

    @Override
    public void onEnable() {
        instance = this;

        // Managers
        this.cooldownManager = new CooldownManager();
        this.abilityManager = new AbilityManager();
        this.teamManager = new TeamManager();
        this.usageManager = new UsageManager();
        this.playerHealthManager = new PlayerHealthManager();
        this.hungerManager = new HungerManager();
        this.partyManager = new PartyManager();
        this.queueManager = new QueueManager(partyManager, arenaManager);
        this.abilitySelectorGUI = new AbilitySelectorGUI(abilityRegistry, abilitySelectionManager);
        this.abilityPersistenceManager = new AbilityPersistenceManager(this);
        this.abilitySelectionManager = new AbilitySelectionManager(abilityRegistry, abilityPersistenceManager);
        Location lobbySpawn = new Location(Bukkit.getWorld("world"), 0, 100, 0);
        this.matchManager = new MatchManager(
                playerHealthManager, teamManager, abilityManager,
                energyManager, hungerManager, scoreboardManager, lobbySpawn
        );
        playerHealthManager.setEliminationCallback(matchManager::onPlayerEliminated);
        // Ability Selector
        this.abilityRegistry = new AbilityRegistry();

        // scoreborard
        this.scoreboardManager = new ScoreboardManager(playerHealthManager);

        // manager
        this.arenaManager = new ArenaManager(
                teamManager,
                abilityManager,
                abilityRegistry,
                abilitySelectionManager,
                cooldownManager,
                usageManager,
                scoreboardManager,
                this, energyManager, playerHealthManager, hungerManager, matchManager
        );

        // Listeners
        getServer().getPluginManager().registerEvents(
                new AbilityTriggerListener(abilityManager), this
        );
        getServer().getPluginManager().registerEvents(
                new InventoryLockListener(), this
        );

        getServer().getPluginManager().registerEvents(
                new CombatListener(teamManager, abilityManager, playerHealthManager), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectionLoadListener(abilitySelectionManager), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectorListener(abilityRegistry, abilitySelectionManager, abilitySelectorGUI), this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new VanillaHungerBlockListener(), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(),this);
        getServer().getPluginManager().registerEvents(
                new MatchDisconnectListener(matchManager, playerHealthManager), this
        );
        for (Player online : Bukkit.getOnlinePlayers()) {
            abilitySelectionManager.loadForPlayer(online);
        }

        // Tasks
        new BaseSpeedTask().runTaskTimer(this, 0L, 10L);
        new EnergyRegenTask(energyManager).runTaskTimer(this, 20L, 20L);
        new HungerTask(hungerManager).runTaskTimer(this, 20L, 20L);
        new AbilityDisplayTask(abilityManager).runTaskTimer(this, 20L, 20L);

        // Commands
        getCommand("party").setExecutor(new PartyCommand(partyManager));
        getCommand("queue").setExecutor(new QueueCommand(queueManager));
        getCommand("abilities").setExecutor(new AbilityMenuCommand(abilitySelectorGUI));
        getLogger().info("ArenaBrawl on.");


    }

    @Override
    public void onDisable() {
        getLogger().info("ArenaBrawl off.");
    }

    public static ArenaBrawlPlugin getInstance() {
        return instance;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public UsageManager getUsageManager() {
        return usageManager;
    }

    public AbilityRegistry getAbilityRegistry() {
        return abilityRegistry;
    }

    public AbilitySelectionManager getAbilitySelectionManager() {
        return abilitySelectionManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public EnergyManager getEnergyManager() {
        return energyManager;
    }

    public PlayerHealthManager getHealthUtils() {
        return playerHealthManager;
    }
}