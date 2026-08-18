// game/ArenaLocations.java
package org.latios.arenaBrawl.game;

import org.bukkit.Location;
import org.bukkit.World;

public class ArenaLocation {

    //WIP
    public static Location redSpawn1(World world) { return new Location(world, 10, 100, 0); }
    public static Location redSpawn2(World world) { return new Location(world, 8, 100, 0); }
    public static Location blueSpawn1(World world) { return new Location(world, -10, 100, 0); }
    public static Location blueSpawn2(World world) { return new Location(world, -8, 100, 0); }
}