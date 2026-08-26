
package org.latios.arenaBrawl.runes;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class RuneSelectionManager {

    private static final RuneType DEFAULT_RUNE = RuneType.DAMAGE;

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, RuneType> selections = new HashMap<>();

    public RuneSelectionManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "runes.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create runes.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadForPlayer(Player player) {
        String saved = config.getString(player.getUniqueId().toString());
        if (saved != null) {
            try {
                selections.put(player.getUniqueId(), RuneType.valueOf(saved));
            } catch (IllegalArgumentException ignored) {
                // stored value no longer matches an existing rune, fall back to default
            }
        }
    }

    public void select(Player player, RuneType rune) {
        selections.put(player.getUniqueId(), rune);
        config.set(player.getUniqueId().toString(), rune.name());
        save();
    }

    public RuneType getSelection(Player player) {
        return selections.getOrDefault(player.getUniqueId(), DEFAULT_RUNE);
    }

    public void unloadPlayer(Player player) {
        selections.remove(player.getUniqueId());
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save runes.yml", e);
        }
    }
}