
package org.latios.arenaBrawl.game;

import org.bukkit.*;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArenaLocation {

    private World arenaWorld;

    private Plugin plugin;

    public ArenaLocation(Plugin plugin) {
        this.plugin = plugin;
        String worldName = plugin.getConfig().getString("worlds.arena", "world");
        plugin.getLogger().info("[ArenaLocation] Configured arena world name: '" + worldName + "'");

        // Check if loaded, otherwise load/create the world from disk
        this.arenaWorld = Bukkit.getWorld(worldName);
        if (this.arenaWorld == null) {
            plugin.getLogger().info("World '" + worldName + "' is not loaded. Attempting to load...");
            WorldCreator creator = new WorldCreator(worldName);
            // creator.environment(World.Environment.NORMAL); // Forces Overworld format
            arenaWorld = Bukkit.createWorld(creator);
            if (this.arenaWorld != null) {
                this.arenaWorld.setDifficulty(Difficulty.NORMAL);
            }
        }

        if (this.arenaWorld == null) {
            plugin.getLogger().severe(
                    "Failed to load arena world '" + worldName + "'! Available worlds: " + Bukkit.getWorlds()
            );
        } else {
            plugin.getLogger().info("Arena world '" + worldName + "' loaded successfully.");
        }
    }

    public List<Location> getPowerupSpawnPoints() {
        List<Location> points = new ArrayList<>();
        List<?> raw = plugin.getConfig().getList("powerup-spawns");
        if (raw == null) return points;

        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> map) {
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                points.add(new Location(getArenaWorld(), x, y, z));
            }
        }
        return points;
    }

    public World getArenaWorld() {
        return arenaWorld;
    }

    public Location getHealthPowerupLocation() {
        var section = plugin.getConfig().getConfigurationSection("powerups.health");
        if (section == null) return null;

        return new Location(getArenaWorld(),
                section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
    }

    public List<Location> getDamagePowerupLocations() {
        List<Location> locations = new ArrayList<>();
        List<?> raw = plugin.getConfig().getList("powerups.damage");
        if (raw == null) return locations;

        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> map) {
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                locations.add(new Location(getArenaWorld(), x, y, z));
            }
        }
        return locations;
    }

    public Location redSpawn1() {
        return new Location(arenaWorld, -2443, 17, 709, 90f, 0f); // Facing West
    }

    public Location redSpawn2() {
        return new Location(arenaWorld, -2443, 17, 708, 90f, 0f); // Facing West
    }

    public Location blueSpawn1() {
        return new Location(arenaWorld, -2499.3, 17, 686, -90f, 0f); // Facing East
    }

    public Location blueSpawn2() {
        return new Location(arenaWorld, -2499.3, 17, 685, -90f, 0f); // Facing East
    }
}