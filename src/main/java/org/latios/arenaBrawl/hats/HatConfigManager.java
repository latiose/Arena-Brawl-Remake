package org.latios.arenaBrawl.hats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class HatConfigManager {

    private final Plugin plugin;
    private final HatRegistry hatRegistry;

    public HatConfigManager(Plugin plugin, HatRegistry hatRegistry) {
        this.plugin = plugin;
        this.hatRegistry = hatRegistry;
    }

    public void loadHats() {
        hatRegistry.clear();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("hats");
        if (root == null) return;

        for (String hatId : root.getKeys(false)) {
            ConfigurationSection hatSection = root.getConfigurationSection(hatId);
            if (hatSection != null) {
                HatDefinition hat = new HatDefinition(hatId, hatSection);
                hatRegistry.register(hat);
            }
        }
    }

    public void reload() {
        plugin.reloadConfig();
        loadHats();
    }
}