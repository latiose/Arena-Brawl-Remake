package org.latios.arenaBrawl.hats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class HatSelectionManager {

    private final Plugin plugin;
    private final HatRegistry hatRegistry;
    private final DatabaseManager db;

    private final Map<UUID, Set<String>> unlockedHats = new HashMap<>();
    private final Map<UUID, String> equippedHat = new HashMap<>();

    public HatSelectionManager(Plugin plugin, HatRegistry hatRegistry, DatabaseManager db) {
        this.plugin = plugin;
        this.hatRegistry = hatRegistry;
        this.db = db;
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        Set<String> unlocked = new HashSet<>();

        String sqlHats = "SELECT hat_id FROM hats WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sqlHats)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    unlocked.add(rs.getString("hat_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load unlocked hats for " + player.getName(), e);
        }
        unlockedHats.put(id, unlocked);

        String sqlEquipped = "SELECT hat_id FROM equipped_hat WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sqlEquipped)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hatId = rs.getString("hat_id");
                    if (hatId != null) {
                        equippedHat.put(id, hatId);
                        player.updateInventory();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load equipped hat for " + player.getName(), e);
        }
    }

    public void unloadPlayer(Player player) {
        unlockedHats.remove(player.getUniqueId());
        equippedHat.remove(player.getUniqueId());
    }

    public boolean isUnlocked(Player player, String hatId) {
        return unlockedHats.getOrDefault(player.getUniqueId(), Set.of()).contains(hatId);
    }

    public boolean unlock(Player player, String hatId) {
        UUID id = player.getUniqueId();
        Set<String> set = unlockedHats.computeIfAbsent(id, k -> new HashSet<>());
        boolean added = set.add(hatId);

        if (added) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String sql = "INSERT OR IGNORE INTO hats (uuid, hat_id) VALUES (?, ?)";
                try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                    ps.setString(1, id.toString());
                    ps.setString(2, hatId);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save unlocked hat for " + id, e);
                }
            });
        }
        return added;
    }

    public void equip(Player player, String hatId) {
        if (!isUnlocked(player, hatId)) return;

        UUID id = player.getUniqueId();
        equippedHat.put(id, hatId);
        player.updateInventory();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO equipped_hat (uuid, hat_id) VALUES (?, ?)
                ON CONFLICT(uuid) DO UPDATE SET hat_id = excluded.hat_id
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.setString(2, hatId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save equipped hat for " + id, e);
            }
        });
    }

    public void unequip(Player player) {
        UUID id = player.getUniqueId();
        equippedHat.remove(player.getUniqueId());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM equipped_hat WHERE uuid = ?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove equipped hat for " + id, e);
            }
        });
    }

    public HatDefinition getEquipped(Player player) {
        String id = equippedHat.get(player.getUniqueId());
        return id != null ? hatRegistry.get(id) : null;
    }

    public Set<String> getUnlockedIds(Player player) {
        return unlockedHats.getOrDefault(player.getUniqueId(), Set.of());
    }
}