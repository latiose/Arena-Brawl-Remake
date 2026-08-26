package org.latios.arenaBrawl.general;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;


public class EntityCleanupUtils {

    public static void markAsArenaEntity(Entity entity) {
        entity.getPersistentDataContainer().set(
                AbilityItemKeys.ARENA_ENTITY_MARKER, PersistentDataType.BYTE, (byte) 1
        );
    }

    public static void sweepArenaEntities(World world) {
        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            boolean hasMarker = entity.getPersistentDataContainer().has(AbilityItemKeys.ARENA_ENTITY_MARKER, PersistentDataType.BYTE);
            boolean isDisplay = entity instanceof ItemDisplay || entity instanceof TextDisplay;

            if (hasMarker || isDisplay) {
                entity.remove();
            }
        }
    }
}