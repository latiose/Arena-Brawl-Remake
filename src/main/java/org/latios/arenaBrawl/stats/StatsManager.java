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

    private final Map<UUID, Integer> matchCoins = new HashMap<>();

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

    public void startMatchTracker(Player player) {
        matchCoins.put(player.getUniqueId(), 0);
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

    public int addWin(Player player) {
        PlayerStats stats = getStats(player);
        stats.wins++;
        int winReward = 80;
        stats.coins += winReward;
        save(player, stats);

        int totalEarnedInMatch = matchCoins.getOrDefault(player.getUniqueId(), 0) + winReward;
        matchCoins.remove(player.getUniqueId());
        return totalEarnedInMatch;
    }

    public int addLoss(Player player) {
        PlayerStats stats = getStats(player);
        stats.losses++;
        int lossReward = 40;
        stats.coins += lossReward;
        save(player, stats);

        int totalEarnedInMatch = matchCoins.getOrDefault(player.getUniqueId(), 0) + lossReward;
        matchCoins.remove(player.getUniqueId());
        return totalEarnedInMatch;
    }

    public void addKill(Player player) {
        PlayerStats stats = getStats(player);
        stats.kills++;
        int killReward = 8;
        stats.coins += killReward;

        matchCoins.put(player.getUniqueId(), matchCoins.getOrDefault(player.getUniqueId(), 0) + killReward);

        save(player, stats);
    }

    public void addDeath(Player player) {
        PlayerStats stats = getStats(player);
        stats.deaths++;
        save(player, stats);
    }

    public void clearMatchTracker(Player player) {
        matchCoins.remove(player.getUniqueId());
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