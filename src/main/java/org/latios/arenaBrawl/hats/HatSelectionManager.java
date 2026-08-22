// hats/HatSelectionManager.java
package org.latios.arenaBrawl.hats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class HatSelectionManager {

    private final Plugin plugin;
    private final HatRegistry hatRegistry;
    private final File file;
    private final YamlConfiguration config;

    private final Map<UUID, Set<String>> unlockedHats = new HashMap<>();
    private final Map<UUID, String> equippedHat = new HashMap<>();

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

        String equipped = config.getString(id + ".equipped");
        if (equipped != null) {
            equippedHat.put(id, equipped);
            player.updateInventory();
        }
    }

    public void unloadPlayer(Player player) {
        unlockedHats.remove(player.getUniqueId());
        equippedHat.remove(player.getUniqueId());
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
        equippedHat.put(player.getUniqueId(), hatId);
        save(player);
        player.updateInventory();
    }

    public void unequip(Player player) {
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

    private void save(Player player) {
        UUID id = player.getUniqueId();
        config.set(id + ".unlocked", new ArrayList<>(unlockedHats.getOrDefault(id, Set.of())));
        config.set(id + ".equipped", equippedHat.get(id));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save hats.yml", e);
        }
    }
}