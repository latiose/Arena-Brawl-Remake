
package org.latios.arenaBrawl.abilities;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.gui.AbilitySelectorListener;
import org.latios.arenaBrawl.hats.HatSelectionManager;
import org.latios.arenaBrawl.hats.KeyManager;
import org.latios.arenaBrawl.rating.RatingManager;
import org.latios.arenaBrawl.runes.RuneSelectionManager;
import org.latios.arenaBrawl.stats.StatsManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

public class AbilitySelectionLoadListener implements Listener {

    private final AbilitySelectionManager selectionManager;
    private final RuneSelectionManager runeManager;
    private final HatSelectionManager hatSelectionManager;
    private final KeyManager keyManager;
    private final CombatUpgradeManager combatUpgradeManager;
    private final MatchManager matchManager;
    private final StatsManager statsManager;
    private final RatingManager ratingManager;
    private final AbilitySelectionManager abilitySelectoManager;
    private final RuneSelectionManager runeSelectionManager;
    private final Plugin plugin;
    public AbilitySelectionLoadListener(AbilitySelectionManager selectionManager, RuneSelectionManager runeManager,HatSelectionManager hatSelectionManager, KeyManager keyManager, CombatUpgradeManager combatUpgradeManager, MatchManager matchManager,
                                        StatsManager statsManager, RatingManager ratingManager, AbilitySelectionManager abilitySelectoManager, RuneSelectionManager runeSelectionManager, Plugin plugin) {
        this.selectionManager = selectionManager;
        this.runeManager = runeManager;
        this.hatSelectionManager = hatSelectionManager;
        this.keyManager = keyManager;
        this.combatUpgradeManager = combatUpgradeManager;
        this.matchManager = matchManager;
        this.statsManager = statsManager;
        this.ratingManager = ratingManager;
        this.abilitySelectoManager = abilitySelectoManager;
        this.runeSelectionManager = runeSelectionManager;
        this.plugin = plugin;
    }


    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            statsManager.loadForPlayer(event.getPlayer());
            ratingManager.loadForPlayer(event.getPlayer());
            keyManager.loadForPlayer(event.getPlayer());
            hatSelectionManager.loadForPlayer(event.getPlayer());
            abilitySelectoManager.loadForPlayer(event.getPlayer());
            runeSelectionManager.loadForPlayer(event.getPlayer());
            combatUpgradeManager.loadForPlayer(event.getPlayer());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            statsManager.unloadPlayer(event.getPlayer());
            ratingManager.unloadPlayer(event.getPlayer());
            keyManager.unloadPlayer(event.getPlayer());
            hatSelectionManager.unloadPlayer(event.getPlayer());
            abilitySelectoManager.unloadPlayer(event.getPlayer());
            runeSelectionManager.unloadPlayer(event.getPlayer());
            combatUpgradeManager.unloadPlayer(event.getPlayer());
        });
    }
}