package org.latios.arenaBrawl.lobby;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LobbyJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        LobbyKit.giveLobbyKit(event.getPlayer());
    }
}