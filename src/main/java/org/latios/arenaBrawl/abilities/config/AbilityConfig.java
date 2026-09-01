package org.latios.arenaBrawl.abilities.config;

import org.bukkit.configuration.ConfigurationSection;

public class AbilityConfig {

    private final ConfigurationSection section; // may be null if this ability has no config entries

    public AbilityConfig(ConfigurationSection section) {
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

    public boolean getBoolean(String key, boolean defaultValue) {
        return section != null ? section.getBoolean(key, defaultValue) : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        return section != null ? section.getString(key, defaultValue) : defaultValue;
    }
}