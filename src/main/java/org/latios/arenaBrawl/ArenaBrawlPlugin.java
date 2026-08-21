package org.latios.arenaBrawl;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.EnergyRegenTask;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.OrbitShieldOrbitTask;
import org.latios.arenaBrawl.abilities.support.OrbitShieldSoundTask;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.debuffs.*;
import org.latios.arenaBrawl.game.ArenaLocation;
import org.latios.arenaBrawl.game.ArenaManager;
import org.latios.arenaBrawl.game.MatchDisconnectListener;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.gui.AbilityMenuCommand;
import org.latios.arenaBrawl.gui.AbilitySelectorGUI;
import org.latios.arenaBrawl.gui.AbilitySelectorListener;
import org.latios.arenaBrawl.lobby.LobbyItemListener;
import org.latios.arenaBrawl.lobby.LobbyJoinListener;
import org.latios.arenaBrawl.party.PartyCommand;
import org.latios.arenaBrawl.party.PartyManager;
import org.latios.arenaBrawl.queue.QueueCommand;
import org.latios.arenaBrawl.queue.QueueManager;
import org.latios.arenaBrawl.rating.LeaderboardCommand;
import org.latios.arenaBrawl.rating.RatingCommand;
import org.latios.arenaBrawl.rating.RatingManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.lang.foreign.Arena;

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
    private ShieldManager shieldManager;
    private ArenaLocation arenaLocation;
    private RatingManager ratingManager;
    private DebuffManager debuffManager;
    private ArmorTierManager armorTierManager;
    private CombatService combatService;
    private OrbitShieldManager orbitShieldManager;

    @Override
    public void onEnable() {
        instance = this;
        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        }
        // Managers
        this.shieldManager = new ShieldManager();
        this.debuffManager = new DebuffManager();

        debuffManager.registerListener(new PolymorphEffectListener());
        //debuffManager.registerListener(new ImmobilizeListener(debuffManager));
        debuffManager.registerListener( new StunListener());
        debuffManager.registerListener( new SlowListener());

        this.ratingManager = new RatingManager(this);
        this.armorTierManager = new ArmorTierManager(ratingManager);
        getCommand("rating").setExecutor(new RatingCommand(ratingManager));
        this.abilityPersistenceManager = new AbilityPersistenceManager(this);
        this.energyManager = new EnergyManager();
        this.cooldownManager = new CooldownManager();
        this.abilityManager = new AbilityManager();
        this.teamManager = new TeamManager();
        this.usageManager = new UsageManager();
        this.playerHealthManager = new PlayerHealthManager();
        this.hungerManager = new HungerManager();
        this.partyManager = new PartyManager();
        this.abilityRegistry = new AbilityRegistry();
        this.orbitShieldManager = new OrbitShieldManager();
        this.combatService = new CombatService(playerHealthManager,shieldManager,debuffManager,orbitShieldManager);
        this.abilitySelectionManager = new AbilitySelectionManager(abilityRegistry, abilityPersistenceManager);
        this.abilitySelectorGUI = new AbilitySelectorGUI(abilityRegistry, abilitySelectionManager);
        this.arenaLocation = new ArenaLocation(this);

        saveDefaultConfig();

        Location lobbySpawn = new Location(Bukkit.getWorld("world"), 0, 100, 0);

        this.matchManager = new MatchManager(
                playerHealthManager, teamManager, abilityManager,
                energyManager, hungerManager, scoreboardManager, lobbySpawn,ratingManager,debuffManager,orbitShieldManager
        );
        playerHealthManager.setEliminationCallback(matchManager::onPlayerEliminated);
        // Ability Selector


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
                this, energyManager, playerHealthManager, hungerManager, matchManager, arenaLocation, shieldManager,debuffManager,armorTierManager,combatService,orbitShieldManager
        );

        this.queueManager = new QueueManager(partyManager, arenaManager);
        // Listeners
        getServer().getPluginManager().registerEvents(
                new AbilityTriggerListener(abilityManager,debuffManager,matchManager), this
        );
        getServer().getPluginManager().registerEvents(
                new InventoryLockListener(), this
        );

        getServer().getPluginManager().registerEvents(
                new CombatListener(teamManager, abilityManager, playerHealthManager,shieldManager,debuffManager,combatService,cooldownManager,matchManager,orbitShieldManager), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectionLoadListener(abilitySelectionManager), this
        );
        getServer().getPluginManager().registerEvents(
                new NaturalRegenListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ProjectileAoeListener(teamManager, combatService), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectorListener(abilityRegistry, abilitySelectionManager, abilitySelectorGUI), this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new VanillaHungerBlockListener(matchManager), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(),this);
        getServer().getPluginManager().registerEvents(
                new MatchDisconnectListener(matchManager, playerHealthManager), this
        );
        getServer().getPluginManager().registerEvents(new ImmobilizeListener(debuffManager), this);
        getServer().getPluginManager().registerEvents(new ImmobilizeJumpListener(debuffManager), this);
      //  getServer().getPluginManager().registerEvents(new StunListener(debuffManager), this);

        for (Player online : Bukkit.getOnlinePlayers()) {
            abilitySelectionManager.loadForPlayer(online);
        }
        getServer().getPluginManager().registerEvents(
                new MobSpawnListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new WeatherListener(), this
        );
        getServer().getPluginManager().registerEvents(new LobbyJoinListener(matchManager,  teamManager,
                 abilityManager,  debuffManager), this);
        getServer().getPluginManager().registerEvents(
                new LobbyItemListener(queueManager, abilitySelectorGUI,matchManager), this
        );

        // Tasks
        new BaseSpeedTask().runTaskTimer(this, 0L, 10L);
        new EnergyRegenTask(energyManager,matchManager).runTaskTimer(this, 20L, 4L);
        new HungerTask(hungerManager,matchManager).runTaskTimer(this, 20L, 20L);
        new AbilityDisplayTask(abilityManager).runTaskTimer(this, 0L, 2L);
        new DebuffTickTask(debuffManager).runTaskTimer(this, 0L, 2L);
        new PolymorphHealTask(debuffManager, playerHealthManager).runTaskTimer(this, 20L, 20L);
        new OrbitShieldOrbitTask(orbitShieldManager).runTaskTimer(this, 0L, 1L);
        new OrbitShieldSoundTask(orbitShieldManager).runTaskTimer(this, 0L, 20L);

        // Commands
        getCommand("party").setExecutor(new PartyCommand(partyManager, queueManager));
        getCommand("queue").setExecutor(new QueueCommand(queueManager, matchManager));
        getCommand("abilities").setExecutor(new AbilityMenuCommand(abilitySelectorGUI, matchManager));
        getCommand("rating").setExecutor(new RatingCommand(ratingManager));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(ratingManager));

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