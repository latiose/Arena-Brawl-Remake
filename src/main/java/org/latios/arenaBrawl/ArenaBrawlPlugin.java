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
import org.latios.arenaBrawl.abilities.ultimate.*;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.debuffs.*;
import org.latios.arenaBrawl.game.*;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.gui.AbilityMenuCommand;
import org.latios.arenaBrawl.gui.AbilitySelectorGUI;
import org.latios.arenaBrawl.gui.AbilitySelectorListener;
import org.latios.arenaBrawl.hats.*;
import org.latios.arenaBrawl.lobby.LobbyItemListener;
import org.latios.arenaBrawl.lobby.LobbyJoinListener;
import org.latios.arenaBrawl.lobby.LobbyScoreboardManager;
import org.latios.arenaBrawl.party.PartyCommand;
import org.latios.arenaBrawl.party.PartyManager;
import org.latios.arenaBrawl.powerups.ArenaCleanupListener;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.powerups.PowerupRotationTask;
import org.latios.arenaBrawl.powerups.PowerupTask;
import org.latios.arenaBrawl.queue.QueueCommand;
import org.latios.arenaBrawl.queue.QueueManager;
import org.latios.arenaBrawl.rating.LeaderboardCommand;
import org.latios.arenaBrawl.rating.RatingCommand;
import org.latios.arenaBrawl.rating.RatingManager;
import org.latios.arenaBrawl.runes.RuneManager;
import org.latios.arenaBrawl.runes.RuneSelectionManager;
import org.latios.arenaBrawl.stats.StatsManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeGUI;
import org.latios.arenaBrawl.upgrades.CombatUpgradeListener;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

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
    private DamageBuffManager damageBuffManager;
    private LobbyScoreboardManager lobbyScoreboardManager;
    private StatsManager statsManager;
    private NametagManager nametagManager;
    private NametagUpdateTask nametagUpdateTask;
    private RuneManager runeManager;
    private RuneSelectionManager runeSelectionManager;
    private HatRegistry hatRegistry;
    private HatSelectionManager hatSelectionManager;
    private KeyManager keyManager;
    private MagicChestManager magicChestManager;
    private MagicChestGUI magicChestGUI;
    private HatPhraseListener hatPhraseListener;
    private HatSelectorGUI hatSelectorGUI;
    private CombatUpgradeManager combatUpgradeManager;
    private CombatUpgradeGUI combatUpgradeGUI;
    private PolymorphNameUpdateTask polymorphNameUpdateTask;
    private BroodMotherEntityManager broodMotherEntityManager;
    @Override
    public void onEnable() {
        instance = this;

        Location lobbySpawn = new Location(Bukkit.getWorld("world"), 0, -60, 0);

        this.arenaLocation = new ArenaLocation(this);
        this.abilityRegistry = new AbilityRegistry(this);
        this.hatRegistry = new HatRegistry();

        this.shieldManager = new ShieldManager();
        this.orbitShieldManager = new OrbitShieldManager();
        this.debuffManager = new DebuffManager();
        this.damageBuffManager = new DamageBuffManager();
        this.energyManager = new EnergyManager();
        this.cooldownManager = new CooldownManager();
        this.abilityManager = new AbilityManager();
        this.teamManager = new TeamManager();
        this.usageManager = new UsageManager();
        this.playerHealthManager = new PlayerHealthManager();
        this.hungerManager = new HungerManager();
        this.partyManager = new PartyManager();

        this.statsManager = new StatsManager(this);
        this.ratingManager = new RatingManager(this);
        this.combatUpgradeManager = new CombatUpgradeManager(this, statsManager);
        this.combatUpgradeGUI = new CombatUpgradeGUI(combatUpgradeManager);
        this.armorTierManager = new ArmorTierManager(ratingManager);
        this.abilityPersistenceManager = new AbilityPersistenceManager(this);
        this.keyManager = new KeyManager(this, statsManager);
        this.nametagManager = new NametagManager(teamManager, playerHealthManager);

        this.lobbyScoreboardManager = new LobbyScoreboardManager(ratingManager, statsManager);
        this.runeSelectionManager = new RuneSelectionManager(this);
        this.runeManager = new RuneManager(energyManager, runeSelectionManager,debuffManager);
        this.abilitySelectionManager = new AbilitySelectionManager(abilityRegistry, abilityPersistenceManager);
        this.hatSelectionManager = new HatSelectionManager(this, hatRegistry);
        this.hatPhraseListener = new HatPhraseListener(hatSelectionManager);

        this.hatSelectorGUI = new HatSelectorGUI(hatRegistry, hatSelectionManager);
        this.abilitySelectorGUI = new AbilitySelectorGUI(abilityRegistry, abilitySelectionManager, runeSelectionManager, hatSelectorGUI, combatUpgradeGUI);
        this.magicChestManager = new MagicChestManager(statsManager, hatRegistry, hatSelectionManager);
        this.magicChestGUI = new MagicChestGUI(keyManager, statsManager);

        this.broodMotherEntityManager = new BroodMotherEntityManager(debuffManager, teamManager, combatService);
        this.matchManager = new MatchManager(
                playerHealthManager, teamManager, abilityManager,
                energyManager, hungerManager, scoreboardManager, lobbySpawn, ratingManager, debuffManager, orbitShieldManager, cooldownManager, usageManager, statsManager, lobbyScoreboardManager, damageBuffManager,
                armorTierManager, hatSelectionManager, broodMotherEntityManager
        );
        this.combatService = new CombatService(
                playerHealthManager, shieldManager, debuffManager, orbitShieldManager, damageBuffManager, matchManager
        );
        this.broodMotherEntityManager.setCombatService(combatService);
        debuffManager.registerListener(new PolymorphEffectListener(teamManager, playerHealthManager));
        debuffManager.registerListener(new StunListener());
        debuffManager.registerListener(new SlowListener());
        saveDefaultConfig();




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
                this, energyManager, playerHealthManager, hungerManager, matchManager, arenaLocation, shieldManager,debuffManager,armorTierManager,combatService,orbitShieldManager, hatSelectionManager,combatUpgradeManager,broodMotherEntityManager
        );

        this.queueManager = new QueueManager(partyManager, arenaManager);
        // Listeners
        getServer().getPluginManager().registerEvents(
                new AbilityTriggerListener(abilityManager,debuffManager,matchManager), this
        );
       getServer().getPluginManager().registerEvents(
                new InventoryLockListener(), this
        );
        getServer().getPluginManager().registerEvents(new BroodMotherHitListener(broodMotherEntityManager), this);

        debuffManager.registerListener(new PoisonListener(playerHealthManager));
       getServer().getPluginManager().registerEvents(
                new CombatUpgradeListener(combatUpgradeGUI, combatUpgradeManager), this
        );



        for (Player online : Bukkit.getOnlinePlayers()) {
            combatUpgradeManager.loadForPlayer(online);
        }

        getServer().getPluginManager().registerEvents(
                new CombatListener(teamManager, abilityManager, playerHealthManager, shieldManager, debuffManager,
                        combatService, cooldownManager, matchManager, orbitShieldManager, runeManager,hatPhraseListener,combatUpgradeManager), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectionLoadListener(abilitySelectionManager,runeSelectionManager,hatSelectionManager,keyManager,combatUpgradeManager,matchManager), this
        );
        getServer().getPluginManager().registerEvents(
                new NaturalRegenListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ProjectileAoeListener(teamManager, combatService), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectorListener(abilityRegistry, abilitySelectionManager, abilitySelectorGUI,runeSelectionManager,hatSelectorGUI,combatUpgradeGUI), this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new VanillaHungerBlockListener(matchManager), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(),this);
        getServer().getPluginManager().registerEvents(new MagicalChestHologramListener(this),this);
        getServer().getPluginManager().registerEvents(
                new MatchDisconnectListener(matchManager, playerHealthManager), this
        );
        getServer().getPluginManager().registerEvents(new ImmobilizeListener(debuffManager), this);
        getServer().getPluginManager().registerEvents(new ImmobilizeJumpListener(debuffManager), this);
        getServer().getPluginManager().registerEvents(
                new HatSelectorListener(hatSelectorGUI, hatSelectionManager), this
        );
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
                 abilityManager,  debuffManager,lobbyScoreboardManager,armorTierManager,hatSelectionManager), this);
        getServer().getPluginManager().registerEvents(
                new LobbyItemListener(queueManager, abilitySelectorGUI,matchManager), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectorListener(abilityRegistry, abilitySelectionManager, abilitySelectorGUI, runeSelectionManager,hatSelectorGUI,combatUpgradeGUI), this
        );
        getServer().getPluginManager().registerEvents(
                new MagicChestListener(magicChestGUI, keyManager, magicChestManager,statsManager), this
        );
        getServer().getPluginManager().registerEvents(new ItemCleanupListener(this), this);
        getServer().getPluginManager().registerEvents(new MobTargetListener(broodMotherEntityManager), this);
        // Tasks
        new BaseSpeedTask().runTaskTimer(this, 0L, 10L);
        new EnergyRegenTask(energyManager,matchManager).runTaskTimer(this, 20L, 5L);
        new HungerTask(hungerManager,matchManager).runTaskTimer(this, 20L, 20L);
        new AbilityDisplayTask(abilityManager).runTaskTimer(this, 0L, 20L);
        new DebuffTickTask(debuffManager).runTaskTimer(this, 0L, 2L);
        new PolymorphHealTask(debuffManager, playerHealthManager).runTaskTimer(this, 20L, 20L);
        new OrbitShieldOrbitTask(orbitShieldManager).runTaskTimer(this, 0L, 1L);
        new OrbitShieldSoundTask(orbitShieldManager).runTaskTimer(this, 0L, 20L);
        new PowerupTask(matchManager, playerHealthManager, damageBuffManager).runTaskTimer(this, 20L, 0L);
        new PowerupRotationTask(matchManager).runTaskTimer(this, 0L, 2L);
        new MatchTimerTask(matchManager).runTaskTimer(this, 20L, 20L);
        new NametagUpdateTask(matchManager, nametagManager).runTaskTimer(this, 0L, 4L);
        new PolymorphNameUpdateTask(debuffManager, playerHealthManager).runTaskTimer(this, 0L, 20L);
        new BroodMotherAI(broodMotherEntityManager).runTaskTimer(this, 0L, 4L);
        Bukkit.getPluginManager().registerEvents(new ItemCleanupListener(this), this);
        // Commands
        getCommand("party").setExecutor(new PartyCommand(partyManager, queueManager));
        getCommand("queue").setExecutor(new QueueCommand(queueManager, matchManager));
        getCommand("abilities").setExecutor(new AbilityMenuCommand(abilitySelectorGUI, matchManager));
        getCommand("rating").setExecutor(new RatingCommand(ratingManager));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(ratingManager));



        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.ADVANCE_WEATHER, false);
            EntityCleanupUtils.sweepArenaEntities(world);
        }
        for (World world : Bukkit.getWorlds()) {
            EntityCleanupUtils.sweepArenaEntities(world); //powerups
        }getServer().getPluginManager().registerEvents(new ArenaCleanupListener(), this);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=minecraft:item_display]"); //powerups stay forever if the server closes mid match idk why
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=minecraft:text_display]");
        for (Player online : Bukkit.getOnlinePlayers()) {
            hatSelectionManager.loadForPlayer(online);
            runeSelectionManager.loadForPlayer(online);
        }
        getLogger().info("ArenaBrawl on.");


    }

    @Override
    public void onDisable() {
        /*
        for (World world : Bukkit.getWorlds()) {
            EntityCleanupUtils.sweepArenaEntities(world); //powerups
        }
        */
        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.ADVANCE_WEATHER, false);
            EntityCleanupUtils.sweepArenaEntities(world);
        }
        for (World world : Bukkit.getWorlds()) {
            EntityCleanupUtils.sweepArenaEntities(world); //powerups
        }
        getLogger().info(

                "ArenaBrawl off.");
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