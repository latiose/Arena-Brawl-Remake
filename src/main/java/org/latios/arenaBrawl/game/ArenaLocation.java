
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




    public List<Location> getLeaderboardSignLocations() {
        List<Location> points = new ArrayList<>();
        List<?> raw = plugin.getConfig().getList("leaderboard-signs");
        if (raw == null) return points;

        World lobbyWorld = Bukkit.getWorld(plugin.getConfig().getString("worlds.lobby", "world"));

        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> map) {
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                points.add(new Location(lobbyWorld, x, y, z));
            }
        }
        return points;
    }


}