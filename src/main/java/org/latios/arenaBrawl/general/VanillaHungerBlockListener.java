// general/VanillaHungerBlockListener.java
package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.latios.arenaBrawl.game.MatchManager;

public class VanillaHungerBlockListener implements Listener {

    private final MatchManager matchManager;

    public VanillaHungerBlockListener(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!matchManager.isInMatch(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (matchManager.isInMatch(player)
                && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }
}