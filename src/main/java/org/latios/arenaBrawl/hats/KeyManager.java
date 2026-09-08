package org.latios.arenaBrawl.hats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.database.DatabaseManager;
import org.latios.arenaBrawl.stats.StatsManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class KeyManager {

    private static final int KEY_COST = 500;

    private final Plugin plugin;
    private final DatabaseManager db;
    private final StatsManager statsManager;
    private final Map<UUID, Integer> keys = new HashMap<>();

    public KeyManager(Plugin plugin, DatabaseManager db, StatsManager statsManager) {
        this.plugin = plugin;
        this.db = db;
        this.statsManager = statsManager;
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        int keyCount = 0;

        String sql = "SELECT key_count FROM keys WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    keyCount = rs.getInt("key_count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load keys for " + player.getName(), e);
        }

        keys.put(id, keyCount);
    }

    public void unloadPlayer(Player player) {
        keys.remove(player.getUniqueId());
    }

    public int getKeys(Player player) {
        return keys.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean buyKey(Player player) {
        var stats = statsManager.getStats(player);
        if (stats.coins < KEY_COST) return false;

        stats.coins -= KEY_COST;
        statsManager.saveDirectly(player, stats);

        int current = getKeys(player);
        keys.put(player.getUniqueId(), current + 1);
        save(player);
        return true;
    }

    public boolean spendKey(Player player) {
        int current = getKeys(player);
        if (current <= 0) return false;

        keys.put(player.getUniqueId(), current - 1);
        save(player);
        return true;
    }

    public static int getKeyCost() { return KEY_COST; }

    private void save(Player player) {
        UUID id = player.getUniqueId();
        int keyCount = getKeys(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO keys (uuid, key_count) VALUES (?, ?)
                ON CONFLICT(uuid) DO UPDATE SET key_count = excluded.key_count
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.setInt(2, keyCount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save keys for " + id, e);
            }
        });
    }
}