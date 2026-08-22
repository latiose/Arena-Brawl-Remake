// abilities/AbilitySelectionLoadListener.java
package org.latios.arenaBrawl.abilities;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.latios.arenaBrawl.hats.HatSelectionManager;
import org.latios.arenaBrawl.hats.KeyManager;
import org.latios.arenaBrawl.runes.RuneSelectionManager;

public class AbilitySelectionLoadListener implements Listener {

    private final AbilitySelectionManager selectionManager;
    private final RuneSelectionManager runeManager;
    private final HatSelectionManager hatSelectionManager;
    private final KeyManager keyManager;
    public AbilitySelectionLoadListener(AbilitySelectionManager selectionManager, RuneSelectionManager runeManager,HatSelectionManager hatSelectionManager, KeyManager keyManager) {
        this.selectionManager = selectionManager;
        this.runeManager = runeManager;
        this.hatSelectionManager = hatSelectionManager;
        this.keyManager = keyManager;
    }


    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        selectionManager.loadForPlayer(event.getPlayer());
        runeManager.loadForPlayer(event.getPlayer());
        hatSelectionManager.loadForPlayer(event.getPlayer());
        keyManager.loadForPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selectionManager.unloadPlayer(event.getPlayer());
        runeManager.unloadPlayer(event.getPlayer());
        hatSelectionManager.unloadPlayer(event.getPlayer());
        keyManager.unloadPlayer(event.getPlayer());
    }
}