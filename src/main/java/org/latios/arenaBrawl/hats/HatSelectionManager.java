
package org.latios.arenaBrawl.hats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class HatSelectionManager {

    /** Persisted marker in hats.yml meaning "wear a random hat each match". */
    public static final String RANDOM_MARKER = "RANDOM";

    private final Plugin plugin;
    private final HatRegistry hatRegistry;
    private final File file;
    private final YamlConfiguration config;

    private final Map<UUID, Set<String>> unlockedHats = new HashMap<>();
    private final Map<UUID, String> equippedHat = new HashMap<>();
    private final Map<UUID, Boolean> randomEquipped = new HashMap<>();

    public HatSelectionManager(Plugin plugin, HatRegistry hatRegistry) {
        this.plugin = plugin;
        this.hatRegistry = hatRegistry;
        this.file = new File(plugin.getDataFolder(), "hats.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create hats.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        List<String> unlocked = config.getStringList(id + ".unlocked");
        unlockedHats.put(id, new HashSet<>(unlocked));

        randomEquipped.remove(id);
        equippedHat.remove(id);
        String equipped = config.getString(id + ".equipped");
        if (RANDOM_MARKER.equals(equipped)) {
            randomEquipped.put(id, true);
        } else if (equipped != null) {
            equippedHat.put(id, equipped);
            player.updateInventory();
        }
    }

    public void unloadPlayer(Player player) {
        unlockedHats.remove(player.getUniqueId());
        equippedHat.remove(player.getUniqueId());
        randomEquipped.remove(player.getUniqueId());
    }

    public boolean isUnlocked(Player player, String hatId) {
        return unlockedHats.getOrDefault(player.getUniqueId(), Set.of()).contains(hatId);
    }

    /** Unlocks a hat for the player. Returns false if they already had it. */
    public boolean unlock(Player player, String hatId) {
        Set<String> set = unlockedHats.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        boolean added = set.add(hatId);
        if (added) save(player);
        return added;
    }

    public void equip(Player player, String hatId) {
        if (!isUnlocked(player, hatId)) return;
        randomEquipped.remove(player.getUniqueId());
        equippedHat.put(player.getUniqueId(), hatId);
        save(player);
        player.updateInventory();
    }

    /** Selects the special "random" option: a hat is rolled for each match. */
    public void equipRandom(Player player) {
        randomEquipped.put(player.getUniqueId(), true);
        equippedHat.remove(player.getUniqueId());
        save(player);
        player.updateInventory();
    }

    public boolean isRandom(Player player) {
        return randomEquipped.getOrDefault(player.getUniqueId(), false);
    }

    public void unequip(Player player) {
        randomEquipped.remove(player.getUniqueId());
        equippedHat.remove(player.getUniqueId());
        save(player);
    }

    public HatDefinition getEquipped(Player player) {
        String id = equippedHat.get(player.getUniqueId());
        return id != null ? hatRegistry.get(id) : null;
    }

    public Set<String> getUnlockedIds(Player player) {
        return unlockedHats.getOrDefault(player.getUniqueId(), Set.of());
    }

    /**
     * Rolls the hat for this match. With "random" selected a (possibly
     * different) hat from the player's unlocked hats is worn every match.
     */
    public void resolveForMatch(Player player) {
        if (!isRandom(player)) {
            return;
        }
        UUID id = player.getUniqueId();
        List<String> ids = new ArrayList<>(getUnlockedIds(player));
        if (ids.isEmpty()) {
            equippedHat.remove(id);
            return;
        }
        equippedHat.put(id, ids.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(ids.size())));
    }

    private void save(Player player) {
        UUID id = player.getUniqueId();
        config.set(id + ".unlocked", new ArrayList<>(unlockedHats.getOrDefault(id, Set.of())));
        config.set(id + ".equipped", isRandom(player) ? RANDOM_MARKER : equippedHat.get(id));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save hats.yml", e);
        }
    }
}