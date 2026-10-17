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
    private int multiplier = 1;
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


    public PlayerStats getStats(Player player) {
        return getStats(player.getUniqueId());
    }

    public PlayerStats getStats(UUID id) {
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
        return addWin(player.getUniqueId());
    }

    public int addWin(UUID id) {
        PlayerStats stats = getStats(id);
        stats.wins++;
        int winReward = 80;
        winReward *= multiplier;
        stats.coins += winReward;
        save(id, stats);

        int totalEarnedInMatch = matchCoins.getOrDefault(id, 0) + winReward;
        matchCoins.remove(id);
        return totalEarnedInMatch;
    }

    public int addLoss(Player player) {
        return addLoss(player.getUniqueId());
    }

    public int addLoss(UUID id) {
        PlayerStats stats = getStats(id);
        stats.losses++;
        int lossReward = 40;
        lossReward *= multiplier;
        stats.coins += lossReward;
        save(id, stats);

        int totalEarnedInMatch = matchCoins.getOrDefault(id, 0) + lossReward;
        matchCoins.remove(id);
        return totalEarnedInMatch;
    }

    public void addKill(Player player) {
        addKill(player.getUniqueId());
    }

    public void addKill(UUID id) {
        PlayerStats stats = getStats(id);
        stats.kills++;
        int killReward = 8;
        killReward *= multiplier;
        stats.coins += killReward;

        matchCoins.put(id, matchCoins.getOrDefault(id, 0) + killReward);

        save(id, stats);
    }

    public void addDeath(Player player) {
        addDeath(player.getUniqueId());
    }

    public void addDeath(UUID id) {
        PlayerStats stats = getStats(id);
        stats.deaths++;
        save(id, stats);
    }

    private void save(Player player, PlayerStats stats) {
        save(player.getUniqueId(), stats);
    }

    private void save(UUID id, PlayerStats stats) {
        String path = id.toString();
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

    public void addCoins(Player target, int amount) {
        PlayerStats stats = getStats(target);
        stats.coins += amount;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }
}