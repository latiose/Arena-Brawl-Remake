package org.latios.arenaBrawl;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.EnergyRegenTask;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;
import org.latios.arenaBrawl.game.ArenaManager;
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
    private HealthUtils healthUtils;
    private HungerManager hungerManager;
    private PartyManager partyManager;
    private QueueManager queueManager;
    private AbilitySelectorGUI abilitySelectorGUI;

    @Override
    public void onEnable() {
        instance = this;

        // Managers
        this.cooldownManager = new CooldownManager();
        this.abilityManager = new AbilityManager();
        this.teamManager = new TeamManager();
        this.usageManager = new UsageManager();
        this.healthUtils = new HealthUtils();
        this.hungerManager = new HungerManager();
        this.partyManager = new PartyManager();
        this.queueManager = new QueueManager(partyManager, arenaManager);
        this.abilitySelectorGUI = new AbilitySelectorGUI(abilityRegistry, abilitySelectionManager);


        // Ability Selector
        this.abilityRegistry = new AbilityRegistry();
        this.abilitySelectionManager = new AbilitySelectionManager(abilityRegistry);

        // scoreborard
        this.scoreboardManager = new ScoreboardManager(healthUtils);

        // manager
        this.arenaManager = new ArenaManager(
                teamManager,
                abilityManager,
                abilityRegistry,
                abilitySelectionManager,
                cooldownManager,
                usageManager,
                scoreboardManager,
                this, energyManager, healthUtils, hungerManager
        );

        // Listeners
        getServer().getPluginManager().registerEvents(
                new AbilityTriggerListener(abilityManager), this
        );
        getServer().getPluginManager().registerEvents(
                new InventoryLockListener(), this
        );

        getServer().getPluginManager().registerEvents(
                new CombatListener(teamManager, abilityManager,healthUtils), this
        );
        getServer().getPluginManager().registerEvents(
                new AbilitySelectorListener(abilityRegistry, abilitySelectionManager, abilitySelectorGUI), this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new VanillaHungerBlockListener(), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(),this);
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

    public  HealthUtils getHealthUtils() {
        return healthUtils;
    }
}