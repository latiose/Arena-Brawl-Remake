// game/MatchDisconnectListener.java
package org.latios.arenaBrawl.game;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.latios.arenaBrawl.general.PlayerHealthManager;

public class MatchDisconnectListener implements Listener {

    private final MatchManager matchManager;
    private final PlayerHealthManager healthManager;

    public MatchDisconnectListener(MatchManager matchManager, PlayerHealthManager healthManager) {
        this.matchManager = matchManager;
        this.healthManager = healthManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!matchManager.isInMatch(player)) return;
        if (healthManager.isEliminated(player)) return;

        healthManager.damage(player, healthManager.getHealth(player));
    }
}