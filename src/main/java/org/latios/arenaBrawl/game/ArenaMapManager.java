
package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Owns the arena map catalog and hands out a map to every new match.
 *
 * <p>With <b>map instancing</b> enabled (the default, mimicking Hypixel-like
 * networks) each configured map is treated as a read-only <i>template</i>. When
 * a match starts, the template world folder is copied to a unique instance
 * folder, loaded as a brand new world and returned to the match. Because each
 * match gets its own isolated world, several matches can run on the same map at
 * once and the template is never mutated. When the match ends the instance
 * world is unloaded and its folder deleted, making the map immediately reusable.</p>
 *
 * <p>Disabling instancing restores the legacy behaviour: one persistent world
 * per map with a single concurrent match each.</p>
 *
 * <p><b>Storage note:</b> modern Paper stores every Bukkit world as a dimension
 * of the main level, e.g. world {@code Bridge} lives at
 * {@code <server>/world/dimensions/minecraft/bridge} rather than
 * {@code <server>/Bridge}. This manager therefore never guesses folder paths:
 * it materialises each template world once, asks the returned {@link World} for
 * its real folder via {@link World#getWorldFolder()} and creates instances as
 * sibling dimensions in the same parent directory.</p>
 */
public class ArenaMapManager {

    /** Capacity reported for maps with no configured instance limit. */
    private static final int UNLIMITED_CAPACITY = 1_000_000;

    /**
     * Files that must never be copied into an instance.
     *
     * <p>{@code metadata.dat} is Paper's per-world metadata and holds the world's
     * unique UUID. Copying it would make every instance share the template's UUID,
     * and Paper refuses to load two worlds with a duplicated UUID ("is a duplicate
     * of another world"). Skipping it lets Paper assign a fresh UUID per instance.
     * The other entries are runtime locks/temporary data that must always be
     * regenerated.</p>
     */
    private static final Set<String> SKIPPED_COPY_FILES = Set.of(
            "session.lock", "uid.dat", "level.dat_old", "metadata.dat"
    );

    private final Plugin plugin;
    private final File worldContainer;
    private final Random random = new Random();
    private final AtomicInteger instanceCounter = new AtomicInteger();

    /** Map templates keyed by their config name. */
    private final Map<String, ArenaMapTemplate> templates = new LinkedHashMap<>();
    /** Real world folder used as the copy source for each template, keyed by map name. */
    private final Map<String, File> templateSourceFolders = new LinkedHashMap<>();
    /** Directory in which instance dimensions are created, keyed by map name. */
    private final Map<String, File> templateInstanceRoots = new LinkedHashMap<>();
    /** Live instance maps currently owned by matches (instancing mode). */
    private final List<ArenaMap> activeInstances = new ArrayList<>();
    /** Folder each live instance world occupies, so it can be deleted on release. */
    private final Map<ArenaMap, File> instanceFolders = new HashMap<>();

    /** Legacy mode state: one persistent world per map, single occupant. */
    private final Map<String, ArenaMap> staticMaps = new LinkedHashMap<>();
    private final Set<String> occupiedMaps = new HashSet<>();

    private final boolean instancingEnabled;
    /** Optional custom template folder; when {@code null} the live world folder is used directly. */
    private final File configuredTemplateFolder;
    private final String instancePrefix;
    private final int maxInstancesPerMap;

    public ArenaMapManager(Plugin plugin) {
        this.plugin = plugin;
        this.worldContainer = Bukkit.getWorldContainer();

        ConfigurationSection instancing = plugin.getConfig().getConfigurationSection("map-instancing");
        this.instancingEnabled = instancing == null || instancing.getBoolean("enabled", true);

        String configuredFolder = instancing == null ? "" : instancing.getString("template-folder", "");
        this.configuredTemplateFolder = configuredFolder == null || configuredFolder.isBlank()
                ? null
                : new File(worldContainer, configuredFolder);

        String configuredPrefix = instancing == null ? null : instancing.getString("instance-prefix");
        this.instancePrefix = configuredPrefix == null || configuredPrefix.isBlank()
                ? "ab_instance_"
                : configuredPrefix;

        this.maxInstancesPerMap = instancing == null ? 0 : instancing.getInt("max-instances-per-map", 0);

        loadMaps();

        if (instancingEnabled) {
            cleanupOrphanInstances();
            plugin.getLogger().info("[ArenaMapManager] Instancing enabled. " + templates.size()
                    + " map template(s) available; each match gets its own world copy.");
        }
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

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
            if (worldName == null || worldName.isBlank()) {
                plugin.getLogger().severe("Map '" + mapName + "' has no 'world' configured. Skipping.");
                continue;
            }

            ArenaMapTemplate template = ArenaMapTemplate.fromConfig(mapName, section);

            if (instancingEnabled) {
                if (!ensureTemplateAvailable(template)) {
                    continue;
                }
                templates.put(mapName, template);
            } else {
                World world = loadPersistentWorld(worldName);
                if (world == null) {
                    continue;
                }
                templates.put(mapName, template);
                staticMaps.put(mapName, template.createMap(world, null));
            }

            plugin.getLogger().info("Loaded arena map: " + mapName
                    + (instancingEnabled ? " (template: " + worldName + ")" : " (world: " + worldName + ")"));
        }
    }

    /** Loads (or creates) a persistent world, used only when instancing is disabled. */
    private World loadPersistentWorld(String worldName) {
        World world = findOrCreateWorld(worldName);
        if (world == null) {
            plugin.getLogger().severe("Map references world '" + worldName + "' which could not be loaded!");
            return null;
        }
        world.setDifficulty(Difficulty.NORMAL);
        return world;
    }

    /**
     * Makes sure a valid template folder exists for the map and that it is not
     * currently loaded as a live world (which would corrupt the copies).
     */
    private boolean ensureTemplateAvailable(ArenaMapTemplate template) {
        String worldName = template.getWorldName();

        World world = findOrCreateWorld(worldName);
        if (world == null) {
            plugin.getLogger().severe("Map '" + template.getName() + "' references world '"
                    + worldName + "' which could not be loaded. This map will be unavailable.");
            return false;
        }

        File liveFolder = world.getWorldFolder();
        if (liveFolder == null || !liveFolder.isDirectory()) {
            plugin.getLogger().severe("Map '" + template.getName() + "' world folder '"
                    + liveFolder + "' does not exist. This map will be unavailable.");
            return false;
        }

        // Instances are sibling dimensions of the live world, so they live next to
        // it inside the same dimensions directory.
        File instanceRoot = liveFolder.getParentFile();
        if (instanceRoot == null) instanceRoot = worldContainer;

        File source = liveFolder;
        if (configuredTemplateFolder != null) {
            source = new File(configuredTemplateFolder, sanitize(worldName));
            if (!source.isDirectory()) {
                try {
                    copyWorldFolder(liveFolder.toPath(), source.toPath());
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create template for map '" + template.getName()
                            + "': " + e.getMessage());
                    return false;
                }
            }
        }

        // A template must never stay loaded as a normal world; the live world was
        // only materialised to guarantee its folder exists on disk.
        unloadWorldObject(world);

        templateSourceFolders.put(template.getName(), source);
        templateInstanceRoots.put(template.getName(), instanceRoot);
        return true;
    }

    /** Finds a loaded world, or loads/creates it from disk. */
    private World findOrCreateWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            for (World candidate : Bukkit.getWorlds()) {
                if (candidate.getName().equalsIgnoreCase(worldName)) {
                    world = candidate;
                    break;
                }
            }
        }
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(worldName));
        }
        return world;
    }

    /** Unloads a world that was only materialised to resolve its folder. */
    private void unloadWorldObject(World world) {
        if (world == null) return;

        List<World> worlds = Bukkit.getWorlds();
        if (!worlds.isEmpty() && world.equals(worlds.getFirst())) return; // never touch the default world

        if (!world.getPlayers().isEmpty()) {
            plugin.getLogger().warning("Template world '" + world.getName()
                    + "' still has players; it will not be unloaded and copies may be inconsistent.");
            return;
        }

        world.save();
        if (Bukkit.unloadWorld(world, true)) {
            plugin.getLogger().info("Unloaded map template world '" + world.getName()
                    + "' (map instancing is active). Remove it from bukkit.yml 'worlds' so it does not"
                    + " load on every startup.");
        }
    }

    // ------------------------------------------------------------------
    // Claiming / releasing
    // ------------------------------------------------------------------

    /** Claims a map valid for the given match type, or {@code null} if none is available. */
    public synchronized ArenaMap claimAvailableMap(MatchType type) {
        return instancingEnabled ? claimInstance(type) : claimStatic(type);
    }

    private ArenaMap claimStatic(MatchType type) {
        List<String> available = new ArrayList<>();
        for (Map.Entry<String, ArenaMap> entry : staticMaps.entrySet()) {
            if (!occupiedMaps.contains(entry.getKey()) && entry.getValue().isValid(type)) {
                available.add(entry.getKey());
            }
        }
        if (available.isEmpty()) return null;

        String chosen = available.get(random.nextInt(available.size()));
        occupiedMaps.add(chosen);
        return staticMaps.get(chosen);
    }

    private ArenaMap claimInstance(MatchType type) {
        List<ArenaMapTemplate> candidates = new ArrayList<>();
        for (ArenaMapTemplate template : templates.values()) {
            if (template.isValid(type) && !isAtCapacity(template)) {
                candidates.add(template);
            }
        }
        if (candidates.isEmpty()) return null;

        Collections.shuffle(candidates, random);
        for (ArenaMapTemplate template : candidates) {
            ArenaMap instance = createInstance(template);
            if (instance != null) return instance;
        }
        return null;
    }

    private boolean isAtCapacity(ArenaMapTemplate template) {
        return maxInstancesPerMap > 0 && activeInstanceCount(template) >= maxInstancesPerMap;
    }

    private int activeInstanceCount(ArenaMapTemplate template) {
        int count = 0;
        for (ArenaMap map : activeInstances) {
            if (map.getTemplate() == template) count++;
        }
        return count;
    }

    /** Releases a map back to the pool, destroying its instance world when applicable. */
    public synchronized void releaseMap(ArenaMap map) {
        if (map == null) return;

        if (map.isInstanced()) {
            activeInstances.remove(map);
            destroyInstance(map);
        } else {
            occupiedMaps.remove(map.getName());
        }
    }

    /**
     * Availability used by the matchmaking queue.
     *
     * @return how many more matches could start for this type right now.
     */
    public int getAvailableMapCount(MatchType type) {
        int total = 0;

        if (instancingEnabled) {
            for (ArenaMapTemplate template : templates.values()) {
                if (!template.isValid(type)) continue;
                if (maxInstancesPerMap <= 0) {
                    total += UNLIMITED_CAPACITY;
                } else {
                    total += Math.max(0, maxInstancesPerMap - activeInstanceCount(template));
                }
                if (total < 0) return Integer.MAX_VALUE;
            }
        } else {
            for (Map.Entry<String, ArenaMap> entry : staticMaps.entrySet()) {
                if (!occupiedMaps.contains(entry.getKey()) && entry.getValue().isValid(type)) total++;
            }
        }
        return total;
    }

    public boolean hasAvailableMap(MatchType type) {
        return getAvailableMapCount(type) > 0;
    }

    // ------------------------------------------------------------------
    // World copies
    // ------------------------------------------------------------------

    private ArenaMap createInstance(ArenaMapTemplate template) {
        File source = templateSourceFolders.get(template.getName());
        File instanceRoot = templateInstanceRoots.get(template.getName());
        if (source == null || instanceRoot == null) {
            plugin.getLogger().severe("No template folder registered for map '" + template.getName() + "'.");
            return null;
        }

        String instanceId = instancePrefix + sanitize(template.getName()) + "_"
                + instanceCounter.incrementAndGet();
        File destination = new File(instanceRoot, instanceId.toLowerCase(Locale.ROOT));

        try {
            copyWorldFolder(source.toPath(), destination.toPath());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to copy template '" + template.getName()
                    + "' into instance '" + instanceId + "': " + e.getMessage());
            deleteFolder(destination);
            return null;
        }

        World world = Bukkit.createWorld(new WorldCreator(instanceId));
        if (world == null) {
            plugin.getLogger().severe("Failed to load arena instance world '" + instanceId + "'.");
            deleteFolder(destination);
            return null;
        }

        world.setDifficulty(Difficulty.NORMAL);
        world.setAutoSave(false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);

        ArenaMap map = template.createMap(world, instanceId);
        activeInstances.add(map);
        instanceFolders.put(map, world.getWorldFolder());
        return map;
    }

    private void destroyInstance(ArenaMap map) {
        World world = map.getWorld();
        File folder = instanceFolders.remove(map);
        if (folder == null && world != null) {
            folder = world.getWorldFolder();
        }

        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        if (folder != null) {
            scheduleDelete(folder, 0);
        }
    }

    private void scheduleDelete(File folder, int attempt) {
        if (attempt == 0) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> tryDelete(folder, 0));
            return;
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> tryDelete(folder, attempt), 20L);
    }

    private void tryDelete(File folder, int attempt) {
        if (deleteFolder(folder)) return;
        if (attempt < 5) {
            scheduleDelete(folder, attempt + 1);
        } else {
            plugin.getLogger().warning("Could not fully delete arena instance folder: " + folder);
        }
    }

    private boolean deleteFolder(File folder) {
        if (!folder.exists()) return true;
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            Iterator<Path> iterator = paths.sorted(java.util.Comparator.reverseOrder()).iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // File may still be locked by the OS; retried by the caller.
                }
            }
        } catch (IOException e) {
            return false;
        }
        return !folder.exists();
    }

    private void copyWorldFolder(Path source, Path destination) throws IOException {
        Files.createDirectories(destination);
        try (Stream<Path> paths = Files.walk(source)) {
            Iterator<Path> iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                Path relative = source.relativize(path);
                if (relative.toString().isEmpty()) continue;

                Path fileName = relative.getFileName();
                if (fileName != null && SKIPPED_COPY_FILES.contains(fileName.toString())) continue;

                Path target = destination.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    try {
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                    } catch (IOException attributeFailure) {
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    /** Removes instance folders left behind by a crash/restart. */
    private void cleanupOrphanInstances() {
        String lowerPrefix = instancePrefix.toLowerCase(Locale.ROOT);
        Set<File> roots = new LinkedHashSet<>(templateInstanceRoots.values());
        roots.add(worldContainer);

        for (File root : roots) {
            File[] files = root.listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (file.isDirectory() && file.getName().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                    plugin.getLogger().info("Removing leftover arena instance: " + file.getName());
                    deleteFolder(file);
                }
            }
        }
    }

    /** Unloads every live instance and deletes its folder. Called on plugin disable. */
    public synchronized void shutdown() {
        for (ArenaMap map : new ArrayList<>(activeInstances)) {
            World world = map.getWorld();
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }
        }
        activeInstances.clear();

        if (!instancingEnabled) return;
        for (File folder : instanceFolders.values()) {
            deleteFolder(folder);
        }
        instanceFolders.clear();
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
