
package org.latios.arenaBrawl.hats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.stats.StatsManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class KeyManager {

    private static final int KEY_COST = 500;

    private final Plugin plugin;
    private final StatsManager statsManager;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Integer> keys = new HashMap<>();

    public KeyManager(Plugin plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.file = new File(plugin.getDataFolder(), "keys.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create keys.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadForPlayer(Player player) {
        keys.put(player.getUniqueId(), config.getInt(player.getUniqueId().toString(), 0));
    }

    public void unloadPlayer(Player player) {
        keys.remove(player.getUniqueId());
    }

    public int getKeys(Player player) {
        return keys.getOrDefault(player.getUniqueId(), 0);
    }

    /** Attempts to buy a key with coins. Returns false if the player can't afford it. */
    public boolean buyKey(Player player) {
        var stats = statsManager.getStats(player);
        if (stats.coins < KEY_COST) return false;

        stats.coins -= KEY_COST;
        statsManager.saveDirectly(player, stats); // see note below

        int current = getKeys(player);
        keys.put(player.getUniqueId(), current + 1);
        save(player);
        return true;
    }

    /** Attempts to spend one key. Returns false if the player has none. */
    public boolean spendKey(Player player) {
        int current = getKeys(player);
        if (current <= 0) return false;

        keys.put(player.getUniqueId(), current - 1);
        save(player);
        return true;
    }

    public static int getKeyCost() { return KEY_COST; }

    private void save(Player player) {
        config.set(player.getUniqueId().toString(), getKeys(player));
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save keys.yml", e);
        }
    }
}