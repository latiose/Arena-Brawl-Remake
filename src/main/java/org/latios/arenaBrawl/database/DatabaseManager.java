package org.latios.arenaBrawl.database;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {

    private final Plugin plugin;
    private Connection connection;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            plugin.getDataFolder().mkdirs();
            File dbFile = new File(plugin.getDataFolder(), "arenabrawl.db");

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");
                statement.execute("PRAGMA journal_mode = WAL;"); // better concurrent read/write performance
            }

            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to the local database", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {

            statement.execute("""
                CREATE TABLE IF NOT EXISTS stats (
                    uuid TEXT PRIMARY KEY,
                    wins INTEGER NOT NULL DEFAULT 0,
                    losses INTEGER NOT NULL DEFAULT 0,
                    kills INTEGER NOT NULL DEFAULT 0,
                    deaths INTEGER NOT NULL DEFAULT 0,
                    coins INTEGER NOT NULL DEFAULT 0
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS ratings (
                    uuid TEXT PRIMARY KEY,
                    rating REAL NOT NULL DEFAULT 1000.0
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS keys (
                    uuid TEXT PRIMARY KEY,
                    key_count INTEGER NOT NULL DEFAULT 0
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS hats (
                    uuid TEXT NOT NULL,
                    hat_id TEXT NOT NULL,
                    PRIMARY KEY (uuid, hat_id)
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS equipped_hat (
                    uuid TEXT PRIMARY KEY,
                    hat_id TEXT
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS abilities (
                    uuid TEXT NOT NULL,
                    slot TEXT NOT NULL,
                    ability_id TEXT NOT NULL,
                    PRIMARY KEY (uuid, slot)
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS runes (
                    uuid TEXT PRIMARY KEY,
                    rune_id TEXT NOT NULL
                );
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS combat_upgrades (
                    uuid TEXT NOT NULL,
                    upgrade_type TEXT NOT NULL,
                    level INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, upgrade_type)
                );
            """);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
        }
    }
}