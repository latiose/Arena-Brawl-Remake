// upgrades/CombatUpgradeManager.java
package org.latios.arenaBrawl.upgrades;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.stats.StatsManager;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CombatUpgradeManager {

    private final Plugin plugin;
    private final StatsManager statsManager;
    private final File file;
    private final YamlConfiguration config;

    private final Map<UUID, Map<CombatUpgradeType, Integer>> levels = new HashMap<>();

    public CombatUpgradeManager(Plugin plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.file = new File(plugin.getDataFolder(), "combat_upgrades.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create combat_upgrades.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();
        Map<CombatUpgradeType, Integer> playerLevels = new EnumMap<>(CombatUpgradeType.class);

        for (CombatUpgradeType type : CombatUpgradeType.values()) {
            int level = config.getInt(id + "." + type.name(), 0);
            playerLevels.put(type, level);
        }

        levels.put(id, playerLevels);
    }

    public void unloadPlayer(Player player) {
        levels.remove(player.getUniqueId());
    }

    public int getLevel(Player player, CombatUpgradeType type) {
        return levels.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(type, 0);
    }

    public double getValue(Player player, CombatUpgradeType type) {
        return type.getValueAtLevel(getLevel(player, type));
    }

    /** Returns the coin cost for the next level, or -1 if already maxed. */
    public int getNextUpgradeCost(Player player, CombatUpgradeType type) {
        int currentLevel = getLevel(player, type);
        if (currentLevel >= CombatUpgradeType.MAX_LEVEL) return -1;
        return CombatUpgradeType.getCostForLevel(currentLevel + 1);
    }

    /**
     * Attempts to purchase the next level of an upgrade.
     * Returns a result describing success/failure so the GUI can react accordingly.
     */
    public PurchaseResult purchase(Player player, CombatUpgradeType type) {
        int currentLevel = getLevel(player, type);
        if (currentLevel >= CombatUpgradeType.MAX_LEVEL) {
            return PurchaseResult.MAXED_OUT;
        }

        int cost = CombatUpgradeType.getCostForLevel(currentLevel + 1);
        var stats = statsManager.getStats(player);

        if (stats.coins < cost) {
            return PurchaseResult.NOT_ENOUGH_COINS;
        }

        stats.coins -= cost;
        statsManager.saveDirectly(player, stats);

        Map<CombatUpgradeType, Integer> playerLevels = levels.computeIfAbsent(
                player.getUniqueId(), k -> new EnumMap<>(CombatUpgradeType.class)
        );
        playerLevels.put(type, currentLevel + 1);
        save(player);

        return PurchaseResult.SUCCESS;
    }

    public enum PurchaseResult {
        SUCCESS, NOT_ENOUGH_COINS, MAXED_OUT
    }

    private void save(Player player) {
        UUID id = player.getUniqueId();
        Map<CombatUpgradeType, Integer> playerLevels = levels.getOrDefault(id, Map.of());

        for (CombatUpgradeType type : CombatUpgradeType.values()) {
            config.set(id + "." + type.name(), playerLevels.getOrDefault(type, 0));
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save combat_upgrades.yml", e);
        }
    }
}