// abilities/AbilitySelectionLoadListener.java
package org.latios.arenaBrawl.abilities;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AbilitySelectionLoadListener implements Listener {

    private final AbilitySelectionManager selectionManager;

    public AbilitySelectionLoadListener(AbilitySelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        selectionManager.loadForPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selectionManager.unloadPlayer(event.getPlayer());
    }
}