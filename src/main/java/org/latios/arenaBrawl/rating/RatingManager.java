
package org.latios.arenaBrawl.rating;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class RatingManager {

    private static final double STARTING_RATING = 1000.0;
    private static final double K_FACTOR = 32.0;
    private static final double MIN_CHANGE = 0.01;
    private static final double MAX_CHANGE = 32.0;

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Double> cache = new HashMap<>();

    public RatingManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ratings.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create ratings.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public double getRating(Player player) {
        UUID id = player.getUniqueId();
        if (cache.containsKey(id)) return cache.get(id);

        double rating = config.getDouble(id.toString(), STARTING_RATING);
        cache.put(id, rating);
        return rating;
    }

    /**
     * Expected win probability of ratingA against ratingB.
     * Standard Elo formula: 1 / (1 + 10^((ratingB - ratingA) / 400))
     */
    public double getWinChance(double ratingA, double ratingB) {
        return 1.0 / (1.0 + Math.pow(10, (ratingB - ratingA) / 400.0));
    }

    /**
     * Rating gain for a single player who won, based only on their OWN rating
     * vs the opponent team's average rating. Clamped between MIN_CHANGE and MAX_CHANGE.
     * Equal ratings (own vs opponent average) always yield exactly +16.
     */
    public double calculateGain(double playerRating, double opponentAverageRating) {
        double c = getWinChance(playerRating, opponentAverageRating);
        double gain = K_FACTOR * (1 - c);
        return clamp(gain, MIN_CHANGE, MAX_CHANGE);
    }

    /**
     * Rating loss for a single player who lost, based only on their OWN rating
     * vs the opponent team's average rating. Clamped between -MAX_CHANGE and -MIN_CHANGE.
     */
    public double calculateLoss(double playerRating, double opponentAverageRating) {
        double d = getWinChance(playerRating, opponentAverageRating);
        double loss = K_FACTOR * (0 - d);
        return -clamp(-loss, MIN_CHANGE, MAX_CHANGE);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public void applyDelta(Player player, double delta) {
        double newRating = getRating(player) + delta;
        cache.put(player.getUniqueId(), newRating);
        config.set(player.getUniqueId().toString(), newRating);
        saveToDisk();
    }

    private void saveToDisk() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save ratings.yml", e);
        }
    }

    public List<Map.Entry<String, Double>> getTopRatings(int limit) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>();

        for (String key : config.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                double rating = config.getDouble(key, STARTING_RATING);
                String name = org.bukkit.Bukkit.getOfflinePlayer(id).getName();
                if (name != null) {
                    entries.add(Map.entry(name, rating));
                }
            } catch (IllegalArgumentException ignored) {

            }
        }

        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }

    public List<LeaderboardEntry> getTopRatingsDetailed(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (String key : config.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                double rating = config.getDouble(key, STARTING_RATING);
                String name = org.bukkit.Bukkit.getOfflinePlayer(id).getName();
                if (name != null) {
                    entries.add(new LeaderboardEntry(id, name, rating));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        entries.sort((a, b) -> Double.compare(b.rating(), a.rating()));

        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }

    public void setRating(Player target, double amount) {
        UUID id = target.getUniqueId();
        cache.put(id, amount);
    }
}