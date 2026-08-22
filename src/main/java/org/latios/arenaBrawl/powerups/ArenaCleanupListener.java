package org.latios.arenaBrawl.powerups;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.latios.arenaBrawl.general.AbilityItemKeys;

public class ArenaCleanupListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            boolean hasMarker = entity.getPersistentDataContainer().has(AbilityItemKeys.ARENA_ENTITY_MARKER, PersistentDataType.BYTE);
            boolean isDisplay = entity instanceof ItemDisplay || entity instanceof TextDisplay;

            if (hasMarker || isDisplay) {
                entity.remove();
            }
        }
    }
}
