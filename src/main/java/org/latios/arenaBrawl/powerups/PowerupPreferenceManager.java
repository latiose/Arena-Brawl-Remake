package org.latios.arenaBrawl.powerups;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stores the per-player choice between the regular damage powerup and the
 * energy powerup. The choice is personal: it only changes how that player
 * sees and uses the damage powerup spawn.
 */
public class PowerupPreferenceManager {

    private static final String ENERGY_MARKER = "ENERGY";

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Set<UUID> energySelections = new HashSet<>();

    public PowerupPreferenceManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "powerups.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create powerups.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        energySelections.remove(id);

        String saved = config.getString(id.toString());
        if (ENERGY_MARKER.equalsIgnoreCase(saved)) {
            energySelections.add(id);
        }
    }

    public boolean usesEnergyPowerup(Player player) {
        return energySelections.contains(player.getUniqueId());
    }

    public void setUsesEnergyPowerup(Player player, boolean energy) {
        UUID id = player.getUniqueId();
        if (energy) {
            energySelections.add(id);
        } else {
            energySelections.remove(id);
        }

        config.set(id.toString(), energy ? ENERGY_MARKER : "DAMAGE");
        save();
    }

    /** Switches the player's powerup and returns the new state (true = energy). */
    public boolean toggle(Player player) {
        boolean energy = !usesEnergyPowerup(player);
        setUsesEnergyPowerup(player, energy);
        return energy;
    }

    public void unloadPlayer(Player player) {
        energySelections.remove(player.getUniqueId());
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save powerups.yml", e);
        }
    }
}
