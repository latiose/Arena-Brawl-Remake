package org.latios.arenaBrawl.general;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.plugin.Plugin;

public class ItemCleanupListener implements Listener {

    private final Plugin plugin;

    public ItemCleanupListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (item.isValid() && !item.isDead()) {
                item.remove();
            }
        }, 200L);
    }
}