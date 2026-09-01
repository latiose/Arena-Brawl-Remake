package org.latios.arenaBrawl.abilities.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class AbilityConfigManager {

    private final Plugin plugin;

    public AbilityConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the config section for a given ability id, under "ability-values.<id>".
     * If no section exists, returns an AbilityConfig that always falls back to defaults.
     */
    public AbilityConfig get(String abilityId) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("ability-values");
        ConfigurationSection section = root != null ? root.getConfigurationSection(abilityId) : null;
        return new AbilityConfig(section);
    }

    /** Call after /reload or a custom reload command to pick up edited values without restarting. */
    public void reload() {
        plugin.reloadConfig();
    }
}