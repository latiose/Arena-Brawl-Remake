
package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArenaLocation {

    private final World arenaWorld;

    private Plugin plugin;
    public ArenaLocation(Plugin plugin) {
        this.plugin = plugin;
        String worldName = plugin.getConfig().getString("worlds.arena", "world");
        this.arenaWorld = Bukkit.getWorld(worldName);

        if (arenaWorld == null) {
            plugin.getLogger().severe(
                    "Arena world '" + worldName + "' not found! Check your config.yml. " +
                            "Available worlds: " + Bukkit.getWorlds()
            );
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
    public Location redSpawn1() { return new Location(arenaWorld, 10, -60, 0); }
    public Location redSpawn2() { return new Location(arenaWorld, 8, -60, 0); }
    public Location blueSpawn1() { return new Location(arenaWorld, -10, -60, 0); }
    public Location blueSpawn2() { return new Location(arenaWorld, -8, -60, 0); }
}