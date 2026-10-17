package org.latios.arenaBrawl.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable description of an arena map ("template").
 *
 * <p>It holds only the raw coordinates from {@code config.yml}, not bound to any
 * {@link World}. When map instancing is enabled there is a single template per
 * map, and many disposable {@link ArenaMap} instances are created from it (one
 * per running match). Because every instance is an exact copy of the template
 * world, all the stored coordinates remain valid without any offset math.</p>
 */
public class ArenaMapTemplate {

    /** A raw coordinate read from config.yml, not bound to any World yet. */
    public record Point(double x, double y, double z, float yaw, float pitch) {
        public Location toLocation(World world) {
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    private final String name;
    private final String worldName;
    private final Point redSpawn1;
    private final Point redSpawn2;
    private final Point blueSpawn1;
    private final Point blueSpawn2;
    private final Point healthPowerup;
    private final List<Point> damagePowerups;
    private final Map<MatchType, List<Point>> spawnsByType;

    public ArenaMapTemplate(String name, String worldName,
                            Point redSpawn1, Point redSpawn2,
                            Point blueSpawn1, Point blueSpawn2,
                            Point healthPowerup, List<Point> damagePowerups,
                            Map<MatchType, List<Point>> spawnsByType) {
        this.name = name;
        this.worldName = worldName;
        this.redSpawn1 = redSpawn1;
        this.redSpawn2 = redSpawn2;
        this.blueSpawn1 = blueSpawn1;
        this.blueSpawn2 = blueSpawn2;
        this.healthPowerup = healthPowerup;
        this.damagePowerups = List.copyOf(damagePowerups);
        this.spawnsByType = new EnumMap<>(MatchType.class);
        spawnsByType.forEach((type, points) -> this.spawnsByType.put(type, List.copyOf(points)));
    }

    public String getName() { return name; }

    /** Name of the world folder this template is copied from. */
    public String getWorldName() { return worldName; }

    public Point getRedSpawn1() { return redSpawn1; }
    public Point getRedSpawn2() { return redSpawn2; }
    public Point getBlueSpawn1() { return blueSpawn1; }
    public Point getBlueSpawn2() { return blueSpawn2; }
    public Point getHealthPowerup() { return healthPowerup; }
    public List<Point> getDamagePowerups() { return damagePowerups; }

    public List<Point> getSpawns(MatchType type) {
        return spawnsByType.getOrDefault(type, List.of());
    }

    public boolean isValid() {
        return redSpawn1 != null && redSpawn2 != null && blueSpawn1 != null && blueSpawn2 != null;
    }

    public boolean isValid(MatchType type) {
        return getSpawns(type).size() >= type.playersNeeded();
    }

    /** Builds a concrete map bound to the given (instance) world. */
    public ArenaMap createMap(World world, String instanceId) {
        return new ArenaMap(this, world, instanceId);
    }

    public static ArenaMapTemplate fromConfig(String name, ConfigurationSection section) {
        String worldName = section.getString("world");
        Point red1 = readPoint(section, "red-spawn-1");
        Point red2 = readPoint(section, "red-spawn-2");
        Point blue1 = readPoint(section, "blue-spawn-1");
        Point blue2 = readPoint(section, "blue-spawn-2");

        return new ArenaMapTemplate(
                name,
                worldName,
                red1,
                red2,
                blue1,
                blue2,
                readPoint(section, "health-powerup"),
                readPointList(section, "damage-powerups"),
                readSpawns(section, red1, red2, blue1, blue2)
        );
    }

    private static Point readPoint(ConfigurationSection section, String key) {
        ConfigurationSection sub = section.getConfigurationSection(key);
        if (sub == null) return null;
        return new Point(
                sub.getDouble("x"),
                sub.getDouble("y"),
                sub.getDouble("z"),
                (float) sub.getDouble("yaw", 0.0),
                (float) sub.getDouble("pitch", 0.0)
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Point> readPointList(ConfigurationSection section, String key) {
        List<Point> points = new ArrayList<>();
        List<?> raw = section.getList(key);
        if (raw == null) return points;

        for (Object entry : raw) {
            if (entry instanceof Map<?, ?> map) {
                double x = ((Number) Objects.requireNonNull(map.get("x"))).doubleValue();
                double y = ((Number) Objects.requireNonNull(map.get("y"))).doubleValue();
                double z = ((Number) Objects.requireNonNull(map.get("z"))).doubleValue();
                float yaw = map.get("yaw") instanceof Number value ? value.floatValue() : 0.0f;
                float pitch = map.get("pitch") instanceof Number value ? value.floatValue() : 0.0f;
                points.add(new Point(x, y, z, yaw, pitch));
            }
        }
        return points;
    }

    private static Map<MatchType, List<Point>> readSpawns(ConfigurationSection section,
                                                          Point red1, Point red2,
                                                          Point blue1, Point blue2) {
        Map<MatchType, List<Point>> spawns = new EnumMap<>(MatchType.class);
        ConfigurationSection modes = section.getConfigurationSection("spawns");

        if (modes != null) {
            for (MatchType type : MatchType.values()) {
                List<Point> points = readPointList(modes, type.getDisplayName());
                if (!points.isEmpty()) spawns.put(type, points);
            }
        }

        List<Point> legacy = Arrays.asList(red1, red2, blue1, blue2);
        if (spawns.get(MatchType.TEAMS) == null && legacy.stream().allMatch(Objects::nonNull)) {
            spawns.put(MatchType.TEAMS, legacy);
        }
        if (spawns.get(MatchType.DUEL) == null && red1 != null && blue1 != null) {
            spawns.put(MatchType.DUEL, List.of(red1, blue1));
        }
        if (spawns.get(MatchType.FFA) == null && legacy.stream().allMatch(Objects::nonNull)) {
            spawns.put(MatchType.FFA, legacy);
        }
        return spawns;
    }
}
