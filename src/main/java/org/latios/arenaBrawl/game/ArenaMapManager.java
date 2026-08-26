
package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class ArenaMapManager {

    private final Plugin plugin;
    private final Map<String, ArenaMap> maps = new LinkedHashMap<>();
    private final Set<String> occupiedMaps = new HashSet<>();
    private final Random random = new Random();

    public ArenaMapManager(Plugin plugin) {
        this.plugin = plugin;
        loadMaps();
    }


    private void loadMaps() {
        ConfigurationSection mapsSection = plugin.getConfig().getConfigurationSection("maps");
        if (mapsSection == null) {
            plugin.getLogger().severe("No 'maps' section found in config.yml!");
            return;
        }

        for (String mapName : mapsSection.getKeys(false)) {
            ConfigurationSection section = mapsSection.getConfigurationSection(mapName);
            if (section == null) continue;

            String worldName = section.getString("world");

            World world = Bukkit.getWorld(worldName);

            if (world == null && worldName != null) {
                world = Bukkit.createWorld(new WorldCreator(worldName));
            }

            if (world == null) {
                plugin.getLogger().severe("Map '" + mapName + "' references world '" + worldName
                        + "' which could not be loaded! This map will be unavailable.");
                continue;
            }
            world.setDifficulty(org.bukkit.Difficulty.NORMAL);

            ArenaMap map = new ArenaMap(
                    mapName,
                    world,
                    readLocation(section, "red-spawn-1", world),
                    readLocation(section, "red-spawn-2", world),
                    readLocation(section, "blue-spawn-1", world),
                    readLocation(section, "blue-spawn-2", world),
                    readLocation(section, "health-powerup", world),
                    readLocationList(section, "damage-powerups", world)
            );

            maps.put(mapName, map);
            plugin.getLogger().info("Loaded arena map: " + mapName);
        }
    }


    private Location readLocation(ConfigurationSection section, String key, World world) {
        var sub = section.getConfigurationSection(key);
        if (sub == null) return null;

        double x = sub.getDouble("x");
        double y = sub.getDouble("y");
        double z = sub.getDouble("z");

        float yaw = (float) sub.getDouble("yaw", 0.0);
        float pitch = (float) sub.getDouble("pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private List<Location> readLocationList(ConfigurationSection section, String key, World world) {
        List<Location> locations = new ArrayList<>();
        List<?> raw = section.getList(key);
        if (raw == null) return locations;

        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> map) {
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                locations.add(new Location(world, x, y, z));
            }
        }
        return locations;
    }

    /** Returns an available (not currently in use) map, or null if all are occupied. */
    public synchronized ArenaMap claimAvailableMap() {
        List<String> availableKeys = new ArrayList<>();
        for (Map.Entry<String, ArenaMap> entry : maps.entrySet()) {
            if (!occupiedMaps.contains(entry.getKey())) {
                availableKeys.add(entry.getKey());
            }
        }
        if (availableKeys.isEmpty()) return null;
        String chosenKey = availableKeys.get(random.nextInt(availableKeys.size()));
        occupiedMaps.add(chosenKey);

        return maps.get(chosenKey);
    }

    public synchronized void releaseMap(ArenaMap map) {
        if (map != null) {
            occupiedMaps.remove(map.getName());
        }
    }

    public int getTotalMapCount() {
        return maps.size();
    }

    public int getAvailableMapCount() {
        return maps.size() - occupiedMaps.size();
    }


}