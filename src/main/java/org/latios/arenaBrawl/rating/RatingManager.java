package org.latios.arenaBrawl.rating;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class RatingManager {

    private static final double STARTING_RATING = 1000.0;
    private static final double K_FACTOR = 32.0;
    private static final double MIN_CHANGE = 0.01;
    private static final double MAX_CHANGE = 32.0;

    private final Plugin plugin;
    private final DatabaseManager db;
    private final Map<UUID, Double> cache = new HashMap<>();

    public RatingManager(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        double rating = STARTING_RATING;

        String sql = "SELECT rating FROM ratings WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) rating = rs.getDouble("rating");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load rating for " + player.getName(), e);
        }

        cache.put(id, rating);
    }

    public void unloadPlayer(Player player) {
        cache.remove(player.getUniqueId());
    }

    public double getRating(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> STARTING_RATING);
    }

    public double getWinChance(double ratingA, double ratingB) {
        return 1.0 / (1.0 + Math.pow(10, (ratingB - ratingA) / 400.0));
    }

    public double calculateGain(double playerRating, double opponentAverageRating) {
        double c = getWinChance(playerRating, opponentAverageRating);
        return clamp(K_FACTOR * (1 - c), MIN_CHANGE, MAX_CHANGE);
    }

    public double calculateLoss(double playerRating, double opponentAverageRating) {
        double d = getWinChance(playerRating, opponentAverageRating);
        return -clamp(K_FACTOR * d, MIN_CHANGE, MAX_CHANGE);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public void applyDelta(Player player, double delta) {
        UUID id = player.getUniqueId();
        double newRating = getRating(player) + delta;
        cache.put(id, newRating);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO ratings (uuid, rating) VALUES (?, ?)
                ON CONFLICT(uuid) DO UPDATE SET rating = excluded.rating
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.setDouble(2, newRating);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save rating for " + id, e);
            }
        });
    }

    /**
     * Fetches the top N players by rating directly from the database — the query itself
     * runs on the calling thread, so call this from an async task (e.g. the 5-minute
     * leaderboard refresh already runs on a scheduled task, not a hot path).
     */
    public List<LeaderboardEntry> getTopRatingsDetailed(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        String sql = "SELECT uuid, rating FROM ratings ORDER BY rating DESC LIMIT ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("uuid"));
                    double rating = rs.getDouble("rating");
                    String name = Bukkit.getOfflinePlayer(id).getName();
                    if (name != null) {
                        entries.add(new LeaderboardEntry(id, name, rating));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not fetch leaderboard", e);
        }

        return entries;
    }
}