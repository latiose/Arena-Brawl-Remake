package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class NaturalRegenListener implements Listener {

    @EventHandler
    public void onNaturalRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();

        if (reason == EntityRegainHealthEvent.RegainReason.SATIATED
                || reason == EntityRegainHealthEvent.RegainReason.EATING
                || reason == EntityRegainHealthEvent.RegainReason.REGEN
                || reason == EntityRegainHealthEvent.RegainReason.MAGIC) {
            event.setCancelled(true);
        }
    }
}