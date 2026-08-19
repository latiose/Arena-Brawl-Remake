
package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class ArenaLocation {

    private final World arenaWorld;

    public ArenaLocation(Plugin plugin) {
        String worldName = plugin.getConfig().getString("worlds.arena", "world");
        this.arenaWorld = Bukkit.getWorld(worldName);

        if (arenaWorld == null) {
            plugin.getLogger().severe(
                    "Arena world '" + worldName + "' not found! Check your config.yml. " +
                            "Available worlds: " + Bukkit.getWorlds()
            );
        }
    }

    public World getArenaWorld() {
        return arenaWorld;
    }

    public Location redSpawn1() { return new Location(arenaWorld, 10, 100, 0); }
    public Location redSpawn2() { return new Location(arenaWorld, 8, 100, 0); }
    public Location blueSpawn1() { return new Location(arenaWorld, -10, 100, 0); }
    public Location blueSpawn2() { return new Location(arenaWorld, -8, 100, 0); }
}