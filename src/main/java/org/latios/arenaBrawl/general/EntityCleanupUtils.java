package org.latios.arenaBrawl.general;

import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;


public class EntityCleanupUtils {

    public static void markAsArenaEntity(Entity entity) {
        entity.getPersistentDataContainer().set(
                AbilityItemKeys.ARENA_ENTITY_MARKER, PersistentDataType.BYTE, (byte) 1
        );
    }

    /** Marks a zombie as an intentional baby so the spawn normalizer leaves it alone. */
    public static void markAllowedBabyZombie(Zombie zombie) {
        zombie.getPersistentDataContainer().set(
                AbilityItemKeys.ALLOW_BABY_ZOMBIE, PersistentDataType.BYTE, (byte) 1
        );
    }

    public static boolean isAllowedBabyZombie(Zombie zombie) {
        return zombie.getPersistentDataContainer().has(
                AbilityItemKeys.ALLOW_BABY_ZOMBIE, PersistentDataType.BYTE
        );
    }

    /**
     * Removes a chicken jockey mount from an entity. Vanilla can turn a
     * freshly spawned zombie into a baby riding a chicken, which the arena
     * must never allow.
     */
    public static void removeChickenJockey(LivingEntity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle instanceof Chicken) {
            entity.eject();
            vehicle.remove();
        }
        for (Entity passenger : new java.util.ArrayList<>(entity.getPassengers())) {
            if (passenger instanceof Chicken) {
                passenger.remove();
            }
        }
    }

    public static void sweepArenaEntities(World world) {
        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            boolean hasMarker = entity.getPersistentDataContainer().has(AbilityItemKeys.ARENA_ENTITY_MARKER, PersistentDataType.BYTE);
            boolean isDisplay = (entity instanceof ItemDisplay || entity instanceof TextDisplay)
                    && hasMarker;

            if (isDisplay) {
                entity.remove();
            }
        }
    }

    /** Removes leftover dropped items from an arena world once its match is over. */
    public static void sweepArenaGroundItems(World world) {
        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.Item) {
                entity.remove();
            }
        }
    }
}