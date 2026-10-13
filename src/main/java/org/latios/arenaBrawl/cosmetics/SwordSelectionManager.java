package org.latios.arenaBrawl.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class SwordSelectionManager {
    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Set<SwordType>> unlocked = new HashMap<>();
    private final Map<UUID, SwordType> equipped = new HashMap<>();

    public SwordSelectionManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "swords.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create swords.yml", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        Set<SwordType> types = new HashSet<>();
        for (String value : config.getStringList(id + ".unlocked")) {
            try {
                types.add(SwordType.valueOf(value));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring unknown sword '" + value + "' for " + player.getName());
            }
        }
        boolean changed = types.add(SwordType.IRON);
        unlocked.put(id, types);

        String equippedName = config.getString(id + ".equipped");
        SwordType selected = parse(equippedName);
        if (selected == null || !types.contains(selected)) {
            selected = SwordType.IRON;
            changed = true;
        }
        equipped.put(id, selected);
        if (changed) save(player);
    }

    public void unloadPlayer(Player player) {
        unlocked.remove(player.getUniqueId());
        equipped.remove(player.getUniqueId());
    }

    public boolean isUnlocked(Player player, SwordType sword) {
        return unlocked.getOrDefault(player.getUniqueId(), Set.of()).contains(sword);
    }

    public boolean unlock(Player player, SwordType sword) {
        boolean added = unlocked.computeIfAbsent(player.getUniqueId(), id -> new HashSet<>()).add(sword);
        if (added) save(player);
        return added;
    }

    public void equip(Player player, SwordType sword) {
        if (!isUnlocked(player, sword)) return;
        equipped.put(player.getUniqueId(), sword);
        save(player);
        player.updateInventory();
    }

    public SwordType getEquipped(Player player) {
        return equipped.getOrDefault(player.getUniqueId(), SwordType.IRON);
    }

    private SwordType parse(String value) {
        if (value == null) return null;
        try {
            return SwordType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void save(Player player) {
        UUID id = player.getUniqueId();
        List<String> values = unlocked.getOrDefault(id, Set.of()).stream().map(Enum::name).sorted().toList();
        config.set(id + ".unlocked", values);
        config.set(id + ".equipped", getEquipped(player).name());
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save swords.yml", e);
        }
    }
}
