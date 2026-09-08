package org.latios.arenaBrawl.stats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StatsManager {

    private final Plugin plugin;
    private final DatabaseManager db;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();

    public StatsManager(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        PlayerStats stats = new PlayerStats();

        String sql = "SELECT wins, losses, kills, deaths, coins FROM stats WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.wins = rs.getInt("wins");
                    stats.losses = rs.getInt("losses");
                    stats.kills = rs.getInt("kills");
                    stats.deaths = rs.getInt("deaths");
                    stats.coins = rs.getInt("coins");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load stats for " + player.getName(), e);
        }

        cache.put(id, stats);
    }

    public void unloadPlayer(Player player) {
        cache.remove(player.getUniqueId());
    }

    public PlayerStats getStats(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerStats());
    }

    public int addWin(Player player) {
        PlayerStats stats = getStats(player);
        stats.wins++;
        int reward = 80;
        earnCoins(stats, reward);
        saveAsync(player, stats);
        return reward;
    }

    public int addLoss(Player player) {
        PlayerStats stats = getStats(player);
        stats.losses++;
        int reward = 40;
        earnCoins(stats, reward);
        saveAsync(player, stats);
        return reward;
    }

    public void addKill(Player player) {
        PlayerStats stats = getStats(player);
        stats.kills++;
        earnCoins(stats, 8);
        saveAsync(player, stats);
    }

    public void addDeath(Player player) {
        PlayerStats stats = getStats(player);
        stats.deaths++;
        saveAsync(player, stats);
    }

    public void addCoins(Player player, int amount) {
        PlayerStats stats = getStats(player);
        earnCoins(stats, amount);
        saveAsync(player, stats);
    }

    public void saveDirectly(Player player, PlayerStats stats) {
        saveAsync(player, stats);
    }

    private void earnCoins(PlayerStats stats, int amount) {
        stats.coins += amount;
    }

    private void saveAsync(Player player, PlayerStats stats) {
        UUID id = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO stats (uuid, wins, losses, kills, deaths, coins)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    wins = excluded.wins,
                    losses = excluded.losses,
                    kills = excluded.kills,
                    deaths = excluded.deaths,
                    coins = excluded.coins
            """;

            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.setInt(2, stats.wins);
                ps.setInt(3, stats.losses);
                ps.setInt(4, stats.kills);
                ps.setInt(5, stats.deaths);
                ps.setInt(6, stats.coins);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save stats for " + id, e);
            }
        });
    }
}