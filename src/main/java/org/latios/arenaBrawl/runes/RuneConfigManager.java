package org.latios.arenaBrawl.runes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class RuneConfigManager {

    private final Plugin plugin;

    public RuneConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public RuneConfig get(String runeId) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("rune-values");
        ConfigurationSection section = root != null ? root.getConfigurationSection(runeId) : null;
        return new RuneConfig(section);
    }

    public void reload() {
        plugin.reloadConfig();
    }
}