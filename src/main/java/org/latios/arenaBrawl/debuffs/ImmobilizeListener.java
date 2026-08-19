
package org.latios.arenaBrawl.debuffs;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ImmobilizeListener implements Listener {

    private final DebuffManager debuffManager;

    public ImmobilizeListener(DebuffManager debuffManager) {
        this.debuffManager = debuffManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!debuffManager.hasDebuff(player, DebuffType.IMMOBILIZE)) return;

        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getZ() != event.getTo().getZ()
                || event.getFrom().getY() != event.getTo().getY()) {
            event.setTo(event.getFrom());
        }
    }
}