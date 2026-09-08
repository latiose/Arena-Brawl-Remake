package org.latios.arenaBrawl.runes;

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

public class RuneSelectionManager {

    private static final RuneType DEFAULT_RUNE = RuneType.DAMAGE;

    private final Plugin plugin;
    private final DatabaseManager db;
    private final Map<UUID, RuneType> selections = new HashMap<>();

    public RuneSelectionManager(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        String sql = "SELECT rune_id FROM runes WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String saved = rs.getString("rune_id");
                    if (saved != null) {
                        try {
                            selections.put(id, RuneType.valueOf(saved));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load rune selection for " + player.getName(), e);
        }
    }

    public void select(Player player, RuneType rune) {
        selections.put(player.getUniqueId(), rune);
        save(player, rune);
    }

    public RuneType getSelection(Player player) {
        return selections.getOrDefault(player.getUniqueId(), DEFAULT_RUNE);
    }

    public void unloadPlayer(Player player) {
        selections.remove(player.getUniqueId());
    }

    private void save(Player player, RuneType rune) {
        UUID id = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO runes (uuid, rune_id) VALUES (?, ?)
                ON CONFLICT(uuid) DO UPDATE SET rune_id = excluded.rune_id
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.setString(2, rune.name());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save rune selection for " + id, e);
            }
        });
    }
}