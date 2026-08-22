// stats/StatsManager.java
package org.latios.arenaBrawl.stats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StatsManager {

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();

    public StatsManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create stats.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public PlayerStats getStats(Player player) {
        UUID id = player.getUniqueId();
        if (cache.containsKey(id)) return cache.get(id);

        PlayerStats stats = new PlayerStats();
        String path = id.toString();
        stats.wins = config.getInt(path + ".wins", 0);
        stats.losses = config.getInt(path + ".losses", 0);
        stats.kills = config.getInt(path + ".kills", 0);
        stats.deaths = config.getInt(path + ".deaths", 0);
        stats.coins = config.getInt(path + ".coins", 0);

        cache.put(id, stats);
        return stats;
    }

    public void saveDirectly(Player player, PlayerStats stats) {
        save(player, stats);
    }

    public void addWin(Player player) {
        PlayerStats stats = getStats(player);
        stats.wins++;
        stats.coins += 80;
        save(player, stats);
    }

    public void addLoss(Player player) {
        PlayerStats stats = getStats(player);
        stats.losses++;
        stats.coins += 40;
        save(player, stats);
    }

    public void addKill(Player player) {
        PlayerStats stats = getStats(player);
        stats.kills++;
        stats.coins += 8;
        save(player, stats);
    }

    public void addDeath(Player player) {
        PlayerStats stats = getStats(player);
        stats.deaths++;
        save(player, stats);
    }

    private void save(Player player, PlayerStats stats) {
        String path = player.getUniqueId().toString();
        config.set(path + ".wins", stats.wins);
        config.set(path + ".losses", stats.losses);
        config.set(path + ".kills", stats.kills);
        config.set(path + ".deaths", stats.deaths);
        config.set(path + ".coins", stats.coins);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save stats.yml", e);
        }
    }
}