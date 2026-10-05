package org.latios.arenaBrawl.abilities.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;

public class AbilityConfigManager {

    private final Plugin plugin;
    private  MatchManager matchManager;
    public AbilityConfigManager(Plugin plugin, MatchManager matchManager) {
        this.plugin = plugin;
        this.matchManager = matchManager;
    }

    /**
     * Returns the config section for a given ability id, under "ability-values.<id>".
     * If no section exists, returns an AbilityConfig that always falls back to defaults.
     */
    public AbilityConfig get(String abilityId) {
        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("ability-values." + abilityId);
        return new AbilityConfig(section,matchManager);
    }

    public void setMatchManager(MatchManager match) {
        this.matchManager = match;
    }
    /** Call after /reload or a custom reload command to pick up edited values without restarting. */
    public void reload() {
        plugin.reloadConfig();
    }
}