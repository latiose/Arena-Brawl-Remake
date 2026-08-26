
package org.latios.arenaBrawl.game;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

public class ArenaMap {

    private final String name;
    private final World world;
    private final Location redSpawn1;
    private final Location redSpawn2;
    private final Location blueSpawn1;
    private final Location blueSpawn2;
    private final Location healthPowerupLocation;
    private final List<Location> damagePowerupLocations;

    public ArenaMap(String name, World world, Location redSpawn1, Location redSpawn2,
                    Location blueSpawn1, Location blueSpawn2,
                    Location healthPowerupLocation, List<Location> damagePowerupLocations) {
        this.name = name;
        this.world = world;
        this.redSpawn1 = redSpawn1;
        this.redSpawn2 = redSpawn2;
        this.blueSpawn1 = blueSpawn1;
        this.blueSpawn2 = blueSpawn2;
        this.healthPowerupLocation = healthPowerupLocation;
        this.damagePowerupLocations = damagePowerupLocations;
    }

    public String getName() { return name; }
    public World getWorld() { return world; }
    public Location getRedSpawn1() { return redSpawn1; }
    public Location getRedSpawn2() { return redSpawn2; }
    public Location getBlueSpawn1() { return blueSpawn1; }
    public Location getBlueSpawn2() { return blueSpawn2; }
    public Location getHealthPowerupLocation() { return healthPowerupLocation; }
    public List<Location> getDamagePowerupLocations() { return damagePowerupLocations; }

    public boolean isValid() {
        return world != null
                && redSpawn1 != null
                && redSpawn2 != null
                && blueSpawn1 != null
                && blueSpawn2 != null;
    }
}