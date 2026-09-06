package org.latios.arenaBrawl.runes;

import org.bukkit.configuration.ConfigurationSection;

public class RuneConfig {

    private final ConfigurationSection section;

    public RuneConfig(ConfigurationSection section) {
        this.section = section;
    }

    public double getDouble(String key, double defaultValue) {
        return section != null ? section.getDouble(key, defaultValue) : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        return section != null ? section.getInt(key, defaultValue) : defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        return section != null ? section.getLong(key, defaultValue) : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        return section != null ? section.getString(key, defaultValue) : defaultValue;
    }
}