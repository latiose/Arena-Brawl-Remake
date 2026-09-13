package org.latios.arenaBrawl;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfigManager;
import org.latios.arenaBrawl.abilities.config.ReloadAbilitiesCommand;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.abilities.cost.EnergyRegenTask;
import org.latios.arenaBrawl.abilities.structures.*;
import org.latios.arenaBrawl.abilities.support.*;
import org.latios.arenaBrawl.abilities.ultimate.*;
import org.latios.arenaBrawl.abilities.utility.Scavenger;
import org.latios.arenaBrawl.abilities.utility.ScavengerManager;
import org.latios.arenaBrawl.abilities.utility.ScavengerMeleeEffect;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.debuffs.*;
import org.latios.arenaBrawl.game.*;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.gui.AbilityMenuCommand;
import org.latios.arenaBrawl.gui.AbilitySelectorGUI;
import org.latios.arenaBrawl.gui.AbilitySelectorListener;
import org.latios.arenaBrawl.hats.*;
import org.latios.arenaBrawl.lobby.*;
import org.latios.arenaBrawl.party.PartyCommand;
import org.latios.arenaBrawl.party.PartyManager;
import org.latios.arenaBrawl.powerups.*;
import org.latios.arenaBrawl.queue.QueueCommand;
import org.latios.arenaBrawl.queue.QueueManager;
import org.latios.arenaBrawl.rating.LeaderboardCommand;
import org.latios.arenaBrawl.rating.RatingCommand;
import org.latios.arenaBrawl.rating.RatingManager;
import org.latios.arenaBrawl.rating.SetRatingCommand;
import org.latios.arenaBrawl.runes.RuneConfigManager;
import org.latios.arenaBrawl.runes.RuneManager;
import org.latios.arenaBrawl.runes.RuneSelectionManager;
import org.latios.arenaBrawl.stats.AddCoinsCommand;
import org.latios.arenaBrawl.stats.StatsManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeGUI;
import org.latios.arenaBrawl.upgrades.CombatUpgradeListener;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;



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
    private ScavengerManager scavengerManager;
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
    private BroodMotherEntityManager broodMotherEntityManager;
    private LeaderboardSignManager leaderboardSignManager;
    private ArenaMapManager arenaMapManager;
    private StructureManager structureManager;
    private MovementLockManager movementLockManager;
    private SongOfPowerManager songOfPowerManager;
    private LifeLeechManager lifeLeechManager;
    private StructureDemolitionService demolitionService;
    private EnergyModifierManager energyModifierManager;
    private EtherealBodyManager etherealBodyManager;
    private DamageVulnerabilityManager damageVulnerabilityManager;
    private AbilityConfigManager abilityConfigManager;
    private RuneConfigManager runeConfigManager;
    private DrawVoteManager drawVoteManager;
    private HatConfigManager hatConfigManager;
    private SpeedBuffManager speedBuffManager;
    private ZombieEntityManager zombieEntityManager;
    @Override
    public void onEnable() {
        instance = this;

        Location lobbySpawn = new Location(Bukkit.getWorld("world"), 0, -60, 0);

        this.arenaLocation = new ArenaLocation(this);
        this.speedBuffManager = new SpeedBuffManager(this);
        this.hatRegistry = new HatRegistry();
        this.hatConfigManager = new HatConfigManager(this,hatRegistry);
        hatConfigManager.loadHats();
        this.abilityConfigManager = new AbilityConfigManager(this);
        this.abilityRegistry = new AbilityRegistry(this,abilityConfigManager);
        this.arenaMapManager = new ArenaMapManager(this);
        this.shieldManager = new ShieldManager();
        this.orbitShieldManager = new OrbitShieldManager();
        this.debuffManager = new DebuffManager();
        this.damageBuffManager = new DamageBuffManager();
        this.energyManager = new EnergyManager();
        this.cooldownManager = new CooldownManager();
        this.abilityManager = new AbilityManager();
        this.teamManager = new TeamManager();
        this.usageManager = new UsageManager();
        this.scavengerManager = new ScavengerManager();
        this.lifeLeechManager = new LifeLeechManager();
        this.damageVulnerabilityManager = new DamageVulnerabilityManager();
        this.playerHealthManager = new PlayerHealthManager(abilityConfigManager.get("rewind"));
        this.hungerManager = new HungerManager();
        this.partyManager = new PartyManager();
        this.movementLockManager = new MovementLockManager();
        this.structureManager = new StructureManager();
        this.movementLockManager = new MovementLockManager();
        this.songOfPowerManager = new SongOfPowerManager();
        this.energyModifierManager = new EnergyModifierManager();
        this.statsManager = new StatsManager(this);
        this.drawVoteManager = new DrawVoteManager();
        this.ratingManager = new RatingManager(this);
        this.etherealBodyManager = new EtherealBodyManager(playerHealthManager);
        this.combatUpgradeManager = new CombatUpgradeManager(this, statsManager);
        this.combatUpgradeGUI = new CombatUpgradeGUI(combatUpgradeManager);
        this.armorTierManager = new ArmorTierManager(ratingManager);
        this.abilityPersistenceManager = new AbilityPersistenceManager(this);
        this.keyManager = new KeyManager(this, statsManager);
        this.nametagManager = new NametagManager(teamManager, playerHealthManager);
        this.leaderboardSignManager = new LeaderboardSignManager(ratingManager, abilityPersistenceManager, abilityRegistry);
        this.runeConfigManager = new RuneConfigManager(this);
        leaderboardSignManager.configureSignLocations(arenaLocation.getLeaderboardSignLocations());

        this.lobbyScoreboardManager = new LobbyScoreboardManager(ratingManager, statsManager);
        this.runeSelectionManager = new RuneSelectionManager(this);
        this.runeManager = new RuneManager(energyManager, runeSelectionManager,debuffManager,runeConfigManager,speedBuffManager);
        this.abilitySelectionManager = new AbilitySelectionManager(abilityRegistry, abilityPersistenceManager);
        this.hatSelectionManager = new HatSelectionManager(this, hatRegistry);
        this.hatPhraseListener = new HatPhraseListener(hatSelectionManager);

        this.hatSelectorGUI = new HatSelectorGUI(hatRegistry, hatSelectionManager);
        this.abilitySelectorGUI = new AbilitySelectorGUI(abilityRegistry, abilitySelectionManager, runeSelectionManager,runeConfigManager);
        this.magicChestManager = new MagicChestManager(statsManager, hatRegistry, hatSelectionManager);
        this.magicChestGUI = new MagicChestGUI(keyManager, statsManager);
        this.zombieEntityManager = new ZombieEntityManager(teamManager,combatService);
        this.broodMotherEntityManager = new BroodMotherEntityManager(debuffManager, teamManager, combatService);
        this.matchManager = new MatchManager(
                playerHealthManager, teamManager, abilityManager, lobbySpawn, ratingManager, debuffManager, orbitShieldManager, cooldownManager, usageManager, statsManager, lobbyScoreboardManager, damageBuffManager,
                armorTierManager, hatSelectionManager, broodMotherEntityManager,this, arenaMapManager,structureManager,songOfPowerManager, drawVoteManager,zombieEntityManager
        );
        this.combatService = new CombatService(
                playerHealthManager, shieldManager, debuffManager, orbitShieldManager, damageBuffManager, matchManager,etherealBodyManager,
                damageVulnerabilityManager
        );
        this.demolitionService = new StructureDemolitionService(structureManager, teamManager);
        this.broodMotherEntityManager.setCombatService(combatService);
        this.zombieEntityManager.setCombatService(combatService);
        combatService.registerMeleeHitEffect(new LifeLeechMeleeEffect(lifeLeechManager, playerHealthManager,abilityConfigManager.get("lifeleech")));
        combatService.registerMeleeHitEffect(new BerserkMeleeEffect());
        combatService.registerMeleeHitEffect(new ScavengerMeleeEffect(scavengerManager,energyManager,abilityConfigManager.get("scavenger")));
        debuffManager.registerListener(new PolymorphEffectListener(playerHealthManager));
        debuffManager.registerListener(new StunListener());
        debuffManager.registerListener(new SilenceListener());
        debuffManager.registerListener(new SlowListener());
        debuffManager.registerListener(new AntiHealListener(playerHealthManager));
        debuffManager.setSongOfPowerManager(songOfPowerManager);
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
                this, energyManager, playerHealthManager, hungerManager, matchManager, shieldManager,debuffManager,armorTierManager,combatService,orbitShieldManager, hatSelectionManager,combatUpgradeManager,broodMotherEntityManager, arenaMapManager,structureManager,movementLockManager,songOfPowerManager,
                lifeLeechManager,demolitionService,energyModifierManager,damageBuffManager,etherealBodyManager, damageVulnerabilityManager,speedBuffManager,zombieEntityManager,
                scavengerManager
        );

        this.queueManager = new QueueManager(this,partyManager, arenaManager,arenaMapManager);
        // Listeners
        getServer().getPluginManager().registerEvents(
                new AbilityTriggerListener(abilityManager,debuffManager,matchManager), this
        );
       getServer().getPluginManager().registerEvents(
                new InventoryLockListener(), this
        );
        getServer().getPluginManager().registerEvents(new BroodMotherHitListener(broodMotherEntityManager,cooldownManager), this);
        getServer().getPluginManager().registerEvents(new ZombieHitListener(zombieEntityManager,cooldownManager), this);
        debuffManager.registerListener(new PoisonListener(playerHealthManager));
        AntiHealListener antiHealListener = new AntiHealListener(playerHealthManager);
        debuffManager.registerListener(antiHealListener);
        Bukkit.getPluginManager().registerEvents(antiHealListener, this);
       getServer().getPluginManager().registerEvents(
                new CombatUpgradeListener(combatUpgradeGUI, combatUpgradeManager), this
        );

       AbilityDependencies abilityDependencies = new AbilityDependencies(
                cooldownManager, teamManager, usageManager, energyManager, shieldManager,
                debuffManager, playerHealthManager, combatService, orbitShieldManager, combatUpgradeManager,broodMotherEntityManager,structureManager,movementLockManager,songOfPowerManager,lifeLeechManager,demolitionService,energyModifierManager,damageBuffManager,
               etherealBodyManager,damageVulnerabilityManager,speedBuffManager,zombieEntityManager,scavengerManager);
        abilityRegistry.setPreviewDependencies(abilityDependencies);
        abilitySelectorGUI.setPreviewDependencies(abilityDependencies);

        for (Player online : Bukkit.getOnlinePlayers()) {
            combatUpgradeManager.loadForPlayer(online);
        }

        getServer().getPluginManager().registerEvents(
                new CombatListener(teamManager, debuffManager,
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
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new VanillaHungerBlockListener(matchManager), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(),this);

        MagicalChestHologramListener hologramListener = new MagicalChestHologramListener(this);
        getServer().getPluginManager().registerEvents(hologramListener, this);

        getServer().getPluginManager().registerEvents(
                new MatchDisconnectListener(matchManager, playerHealthManager), this
        );
        getServer().getPluginManager().registerEvents(
                new BlockInteractionListener(), this
        );
        getServer().getPluginManager().registerEvents(new ImmobilizeListener(debuffManager,movementLockManager), this);
        getServer().getPluginManager().registerEvents(new ImmobilizeJumpListener(debuffManager), this);
        getServer().getPluginManager().registerEvents(
                new HatSelectorListener(hatSelectorGUI, hatSelectionManager), this
        );
        getServer().getPluginManager().registerEvents(
                new OrbitShieldHitListener(orbitShieldManager, combatService,teamManager,cooldownManager,debuffManager),
                this
        );
       //getServer().getPluginManager().registerEvents(new StunListener(debuffManager), this);

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
                 abilityManager,  debuffManager,lobbyScoreboardManager,armorTierManager,hatSelectionManager,hologramListener,this), this);
        getServer().getPluginManager().registerEvents(
                new LobbyItemListener(queueManager, abilitySelectorGUI,matchManager), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectorListener(abilityRegistry, abilitySelectionManager, abilitySelectorGUI, runeSelectionManager,hatSelectorGUI,combatUpgradeGUI), this
        );
        getServer().getPluginManager().registerEvents(
                new MagicChestListener(magicChestGUI, keyManager, magicChestManager,statsManager), this
        );
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                .registerListener(new AttackSoundBlockListener());
        getServer().getPluginManager().registerEvents(new StructureHitListener(structureManager), this);
        getServer().getPluginManager().registerEvents(new ItemCleanupListener(this), this);
        getServer().getPluginManager().registerEvents(new MobTargetListener(broodMotherEntityManager), this);
        // Tasks
        new BaseSpeedTask(speedBuffManager).runTaskTimer(this, 0L, 10L);
        new EnergyRegenTask(energyManager,matchManager,energyModifierManager).runTaskTimer(this, 20L, 5L);
        new HungerTask(hungerManager,matchManager,songOfPowerManager).runTaskTimer(this, 20L, 20L);
        new AbilityDisplayTask(abilityManager).runTaskTimer(this, 0L, 2L);
        new DebuffTickTask(debuffManager).runTaskTimer(this, 0L, 2L);
        new PolymorphHealTask(debuffManager, playerHealthManager).runTaskTimer(this, 20L, 20L);
        new OrbitShieldOrbitTask(orbitShieldManager).runTaskTimer(this, 0L, 1L);
        new OrbitShieldSoundTask(orbitShieldManager).runTaskTimer(this, 0L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> {shieldManager.tick();;}, 0L, 1L);
        new PowerupTask(matchManager, playerHealthManager, damageBuffManager).runTaskTimer(this, 20L, 0L);
        new PowerupRotationTask(matchManager).runTaskTimer(this, 0L, 2L);
        new MatchTimerTask(matchManager).runTaskTimer(this, 20L, 20L);
        new NametagUpdateTask(matchManager, nametagManager).runTaskTimer(this, 0L, 4L);
        new PolymorphNameUpdateTask(debuffManager, playerHealthManager).runTaskTimer(this, 0L, 20L);
        new BroodMotherAI(broodMotherEntityManager).runTaskTimer(this, 0L, 4L);
        new ZombieAI(zombieEntityManager).runTaskTimer(this, 0L, 4L);
        new LeaderboardRefreshTask(leaderboardSignManager).runTaskTimer(this, 20L, 20L * 60 * 5);
        new LeaderboardRotationTask(leaderboardSignManager).runTaskTimer(this, 20L * 6, 20L * 2);
        new DamageBuffParticleTask(damageBuffManager).runTaskTimer(this, 0L, 20L);
        new StructureTickTask(structureManager).runTaskTimer(this, 0L, 20L);

        Bukkit.getPluginManager().registerEvents(new ItemCleanupListener(this), this);

        // Commands
        getCommand("party").setExecutor(new PartyCommand(partyManager, queueManager));
        getCommand("queue").setExecutor(new QueueCommand(queueManager, matchManager));
        getCommand("abilities").setExecutor(new AbilityMenuCommand(abilitySelectorGUI, matchManager));
        getCommand("draw").setExecutor(new DrawCommand(matchManager, drawVoteManager));
        getCommand("rating").setExecutor(new RatingCommand(ratingManager));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(ratingManager));
        getCommand("reloadabilities").setExecutor(
                new ReloadAbilitiesCommand(abilityConfigManager, abilityRegistry, abilitySelectorGUI,hatConfigManager,hatSelectorGUI,runeConfigManager)
        );
        getCommand("capturestructure").setExecutor(new org.latios.arenaBrawl.abilities.structures.CaptureStructureCommand(this));
        getCommand("addcoins").setExecutor(new AddCoinsCommand(statsManager,lobbyScoreboardManager));
        getCommand("setrating").setExecutor(new SetRatingCommand(ratingManager,lobbyScoreboardManager));

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
        structureManager.clearAll();
        getLogger().info("ArenaBrawl on.");


    }

    @Override
    public void onDisable() {
        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.ADVANCE_WEATHER, false);
            EntityCleanupUtils.sweepArenaEntities(world);
        }
        for (World world : Bukkit.getWorlds()) {
            EntityCleanupUtils.sweepArenaEntities(world); //powerups
        }
        structureManager.clearAll();
        getLogger().info(

                "ArenaBrawl off.");
    }



    public static ArenaBrawlPlugin getInstance() {
        return instance;
    }



    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public EnergyManager getEnergyManager() {
        return energyManager;
    }

}