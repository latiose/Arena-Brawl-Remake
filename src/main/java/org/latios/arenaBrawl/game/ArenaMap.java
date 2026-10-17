
package org.latios.arenaBrawl.game;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A concrete, live arena map bound to a specific {@link World}.
 *
 * <p>When map instancing is enabled, each running match owns its own
 * {@code ArenaMap} whose world is a disposable copy of an
 * {@link ArenaMapTemplate}. All coordinates are rebuilt from the template so the
 * rest of the plugin can keep working with plain {@link Location}s.</p>
 */
public class ArenaMap {

    private final ArenaMapTemplate template;
    private final String name;
    private final World world;
    private final String instanceId;
    private final Location redSpawn1;
    private final Location redSpawn2;
    private final Location blueSpawn1;
    private final Location blueSpawn2;
    private final Location healthPowerupLocation;
    private final List<Location> damagePowerupLocations;
    private final Map<MatchType, List<Location>> spawnsByType;

    public ArenaMap(ArenaMapTemplate template, World world, String instanceId) {
        this.template = template;
        this.name = template.getName();
        this.world = world;
        this.instanceId = instanceId;

        this.redSpawn1 = toLocation(template.getRedSpawn1());
        this.redSpawn2 = toLocation(template.getRedSpawn2());
        this.blueSpawn1 = toLocation(template.getBlueSpawn1());
        this.blueSpawn2 = toLocation(template.getBlueSpawn2());
        this.healthPowerupLocation = toLocation(template.getHealthPowerup());
        this.damagePowerupLocations = template.getDamagePowerups().stream()
                .map(this::toLocation)
                .toList();

        this.spawnsByType = new EnumMap<>(MatchType.class);
        for (MatchType type : MatchType.values()) {
            List<Location> locations = template.getSpawns(type).stream()
                    .map(this::toLocation)
                    .toList();
            if (!locations.isEmpty()) spawnsByType.put(type, locations);
        }
    }

    private Location toLocation(ArenaMapTemplate.Point point) {
        return point == null ? null : point.toLocation(world);
    }

    public String getName() { return name; }
    public World getWorld() { return world; }
    public ArenaMapTemplate getTemplate() { return template; }

    /** Unique id of the disposable instance world, or {@code null} when instancing is disabled. */
    public String getInstanceId() { return instanceId; }
    public boolean isInstanced() { return instanceId != null; }

    public Location getRedSpawn1() { return redSpawn1; }
    public Location getRedSpawn2() { return redSpawn2; }
    public Location getBlueSpawn1() { return blueSpawn1; }
    public Location getBlueSpawn2() { return blueSpawn2; }
    public Location getHealthPowerupLocation() { return healthPowerupLocation; }
    public List<Location> getDamagePowerupLocations() { return damagePowerupLocations; }

    public List<Location> getSpawns(MatchType type) {
        return spawnsByType.getOrDefault(type, List.of());
    }

    public boolean isValid() {
        return world != null
                && redSpawn1 != null
                && redSpawn2 != null
                && blueSpawn1 != null
                && blueSpawn2 != null;
    }

    public boolean isValid(MatchType type) {
        return getSpawns(type).size() >= type.playersNeeded();
    }
}
