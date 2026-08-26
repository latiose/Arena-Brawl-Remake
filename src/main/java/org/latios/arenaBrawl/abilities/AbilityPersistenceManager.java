
package org.latios.arenaBrawl.abilities;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AbilityPersistenceManager {

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration config;

    public AbilityPersistenceManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "abilities.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create abilities.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save(Player player, AbilitySlot slot, String abilityId) {
        String path = player.getUniqueId() + "." + slot.name();
        config.set(path, abilityId);
        saveToDisk();
    }

    public String load(UUID playerId, AbilitySlot slot) {
        return config.getString(playerId + "." + slot.name(), null);
    }

    public Map<AbilitySlot, String> loadAll(UUID playerId) {
        Map<AbilitySlot, String> result = new EnumMap<>(AbilitySlot.class);
        for (AbilitySlot slot : AbilitySlot.values()) {
            String id = load(playerId, slot);
            if (id != null) {
                result.put(slot, id);
            }
        }
        return result;
    }

    private void saveToDisk() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save abilities.yml", e);
        }
    }
}