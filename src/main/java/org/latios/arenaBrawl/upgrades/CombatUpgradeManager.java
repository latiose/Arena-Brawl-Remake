package org.latios.arenaBrawl.upgrades;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.database.DatabaseManager;
import org.latios.arenaBrawl.stats.StatsManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CombatUpgradeManager {

    private final Plugin plugin;
    private final DatabaseManager db;
    private final StatsManager statsManager;

    private final Map<UUID, Map<CombatUpgradeType, Integer>> levels = new HashMap<>();

    public CombatUpgradeManager(Plugin plugin, DatabaseManager db, StatsManager statsManager) {
        this.plugin = plugin;
        this.db = db;
        this.statsManager = statsManager;
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        Map<CombatUpgradeType, Integer> playerLevels = new EnumMap<>(CombatUpgradeType.class);

        String sql = "SELECT upgrade_type, level FROM combat_upgrades WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        CombatUpgradeType type = CombatUpgradeType.valueOf(rs.getString("upgrade_type"));
                        playerLevels.put(type, rs.getInt("level"));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load combat upgrades for " + player.getName(), e);
        }

        levels.put(id, playerLevels);
    }

    public void unloadPlayer(Player player) {
        levels.remove(player.getUniqueId());
    }

    public int getLevel(Player player, CombatUpgradeType type) {
        return levels.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(type, 0);
    }

    public double getValue(Player player, CombatUpgradeType type) {
        return type.getValueAtLevel(getLevel(player, type));
    }

    public int getNextUpgradeCost(Player player, CombatUpgradeType type) {
        int currentLevel = getLevel(player, type);
        if (currentLevel >= CombatUpgradeType.MAX_LEVEL) return -1;
        return CombatUpgradeType.getCostForLevel(currentLevel + 1);
    }

    public PurchaseResult purchase(Player player, CombatUpgradeType type) {
        int currentLevel = getLevel(player, type);
        if (currentLevel >= CombatUpgradeType.MAX_LEVEL) {
            return PurchaseResult.MAXED_OUT;
        }

        int cost = CombatUpgradeType.getCostForLevel(currentLevel + 1);
        var stats = statsManager.getStats(player);

        if (stats.coins < cost) {
            return PurchaseResult.NOT_ENOUGH_COINS;
        }

        stats.coins -= cost;
        statsManager.saveDirectly(player, stats);

        Map<CombatUpgradeType, Integer> playerLevels = levels.computeIfAbsent(
                player.getUniqueId(), k -> new EnumMap<>(CombatUpgradeType.class)
        );
        int newLevel = currentLevel + 1;
        playerLevels.put(type, newLevel);
        save(player, type, newLevel);

        return PurchaseResult.SUCCESS;
    }

    public enum PurchaseResult {
        SUCCESS, NOT_ENOUGH_COINS, MAXED_OUT
    }

    private void save(Player player, CombatUpgradeType type, int level) {
        UUID id = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO combat_upgrades (uuid, upgrade_type, level) VALUES (?, ?, ?)
                ON CONFLICT(uuid, upgrade_type) DO UPDATE SET level = excluded.level
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.setString(2, type.name());
                ps.setInt(3, level);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save combat upgrade for " + id, e);
            }
        });
    }
}