// debuffs/ImmobilizeJumpListener.java
package org.latios.arenaBrawl.debuffs;


import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ImmobilizeJumpListener implements Listener {

    private final DebuffManager debuffManager;

    public ImmobilizeJumpListener(DebuffManager debuffManager) {
        this.debuffManager = debuffManager;
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();

        if (isCurrentlyImmobilizing(player)) {
            event.setCancelled(true);
        }
    }

    private boolean isCurrentlyImmobilizing(Player player) {
        for (DebuffType type : DebuffType.values()) {
            if (type.isImmobilizing() && debuffManager.hasDebuff(player, type)) {
                return true;
            }
        }
        return false;
    }
}