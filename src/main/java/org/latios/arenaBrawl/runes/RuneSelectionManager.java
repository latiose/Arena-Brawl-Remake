
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

    /** Persisted marker in runes.yml meaning "roll a random rune each match". */
    public static final String RANDOM_MARKER = "RANDOM";

    private static final RuneType DEFAULT_RUNE = RuneType.DAMAGE;

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, RuneType> selections = new HashMap<>();
    private final Map<UUID, Boolean> randomSelections = new HashMap<>();
    private final Map<UUID, RuneType> matchRolls = new HashMap<>();

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
        UUID id = player.getUniqueId();
        randomSelections.remove(id);
        matchRolls.remove(id);

        String saved = config.getString(id.toString());
        if (saved == null) {
            return;
        }
        if (RANDOM_MARKER.equals(saved)) {
            randomSelections.put(id, true);
            return;
        }
        try {
            selections.put(id, RuneType.valueOf(saved));
        } catch (IllegalArgumentException ignored) {
            // stored value no longer matches an existing rune, fall back to default
        }
    }

    public void select(Player player, RuneType rune) {
        UUID id = player.getUniqueId();
        selections.put(id, rune);
        randomSelections.remove(id);
        // Drop any roll from a previous match so it cannot override the new choice.
        matchRolls.remove(id);
        config.set(id.toString(), rune.name());
        save();
    }

    /** Selects the special "random" option: a rune is rolled for each match. */
    public void selectRandom(Player player) {
        UUID id = player.getUniqueId();
        selections.remove(id);
        randomSelections.put(id, true);
        // Drop any roll from a previous match so the next match rolls a fresh rune.
        matchRolls.remove(id);
        config.set(id.toString(), RANDOM_MARKER);
        save();
    }

    public boolean isRandom(Player player) {
        return randomSelections.getOrDefault(player.getUniqueId(), false);
    }

    public RuneType getSelection(Player player) {
        // During a match the rolled rune is used for procs.
        RuneType roll = matchRolls.get(player.getUniqueId());
        if (roll != null) {
            return roll;
        }
        if (isRandom(player)) {
            return DEFAULT_RUNE;
        }
        return selections.getOrDefault(player.getUniqueId(), DEFAULT_RUNE);
    }

    /**
     * Rolls the rune for this match. With "random" selected this yields a
     * (possibly different) rune every match; otherwise the saved rune.
     */
    public RuneType resolveForMatch(Player player) {
        if (!isRandom(player)) {
            // A non-random player must never keep a roll from an earlier random match.
            matchRolls.remove(player.getUniqueId());
            return getSelection(player);
        }
        RuneType[] runes = RuneType.values();
        RuneType rolled = runes[java.util.concurrent.ThreadLocalRandom.current().nextInt(runes.length)];
        matchRolls.put(player.getUniqueId(), rolled);
        return rolled;
    }

    public void unloadPlayer(Player player) {
        selections.remove(player.getUniqueId());
        randomSelections.remove(player.getUniqueId());
        matchRolls.remove(player.getUniqueId());
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save runes.yml", e);
        }
    }
}