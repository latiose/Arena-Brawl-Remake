package org.latios.arenaBrawl.abilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AbilityPersistenceManager {

    private final Plugin plugin;
    private final DatabaseManager db;

    public AbilityPersistenceManager(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void save(Player player, AbilitySlot slot, String abilityId) {
        UUID id = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                INSERT INTO abilities (uuid, slot, ability_id) VALUES (?, ?, ?)
                ON CONFLICT(uuid, slot) DO UPDATE SET ability_id = excluded.ability_id
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, id.toString());
                ps.setString(2, slot.name());
                ps.setString(3, abilityId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save ability for " + id, e);
            }
        });
    }

    public String load(UUID playerId, AbilitySlot slot) {
        String sql = "SELECT ability_id FROM abilities WHERE uuid = ? AND slot = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, slot.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ability_id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load ability for " + playerId, e);
        }
        return null;
    }

    public Map<AbilitySlot, String> loadAll(UUID playerId) {
        Map<AbilitySlot, String> result = new EnumMap<>(AbilitySlot.class);
        String sql = "SELECT slot, ability_id FROM abilities WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        AbilitySlot slot = AbilitySlot.valueOf(rs.getString("slot"));
                        result.put(slot, rs.getString("ability_id"));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load all abilities for " + playerId, e);
        }
        return result;
    }
}