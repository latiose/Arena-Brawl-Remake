// powerups/PowerupManager.java
package org.latios.arenaBrawl.powerups;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.latios.arenaBrawl.general.EntityCleanupUtils;

import java.util.*;

public class PowerupManager {

    private static final double PICKUP_RADIUS = 2;
    private static final float ITEM_SCALE = 0.4f;
    private static final double ROTATION_SPEED_PER_TICK = 0.1;
    private static final double HOLOGRAM_HEIGHT_OFFSET = 0.6;

    private static class PowerupState {
        List<Location> locations = List.of();
        ItemDisplay itemEntity;
        TextDisplay hologramEntity;
        Location spawnedAt;
        double rotationAngle = 0.0;


        long lastHandledCycleStart = -1;

        boolean pickedUpThisWindow = false;
    }

    private final Map<PowerupType, PowerupState> states = new EnumMap<>(PowerupType.class);
    private final Random random = new Random();

    public PowerupManager() {
        for (PowerupType type : PowerupType.values()) {
            states.put(type, new PowerupState());
        }
    }

    public void configureLocations(PowerupType type, List<Location> locations) {
        states.get(type).locations = locations.stream()
                .filter(loc -> loc.getWorld() != null)
                .peek(loc -> {
                    if (!loc.getChunk().isLoaded()) {
                        loc.getChunk().load();
                    }
                })
                .toList();
    }

    public void reset() {
        for (PowerupState state : states.values()) {
            despawn(state);
            state.lastHandledCycleStart = -1;
            state.pickedUpThisWindow = false;
        }
    }

    public void clear() {
        reset();
    }

    /** Call once per second with the match's elapsed time in millis. */
    public void tick(long matchElapsedMillis, Collection<Player> players) {
        for (PowerupType type : PowerupType.values()) {
            PowerupState state = states.get(type);
            if (state.locations.isEmpty()) continue;

            PowerupSchedule schedule = type.getSchedule();
            long cycleLength = schedule.cycleMillis();
            long cyclePosition = matchElapsedMillis % cycleLength;
            long currentCycleStart = matchElapsedMillis - cyclePosition;

            boolean inWindow = schedule.isWithinWindow(cyclePosition);
            boolean entityStillAlive = state.itemEntity != null && !state.itemEntity.isDead();

            if (currentCycleStart != state.lastHandledCycleStart) {
                state.pickedUpThisWindow = false;
            }

            if (inWindow) {
                if (!entityStillAlive && !state.pickedUpThisWindow && state.lastHandledCycleStart != currentCycleStart) {
                    spawn(type, state);
                    state.lastHandledCycleStart = currentCycleStart;
                }
            }
        }
    }

    /** Call from CheckPickups when a powerup is collected, so it won't respawn until the next window. */
    private void markPickedUp(PowerupState state) {
        state.pickedUpThisWindow = true;
    }

    public void tickRotation() {
        for (PowerupState state : states.values()) {
            if (state.itemEntity == null || state.itemEntity.isDead()) continue;

            state.rotationAngle += ROTATION_SPEED_PER_TICK;

            Transformation transformation = state.itemEntity.getTransformation();
            transformation.getLeftRotation().identity().rotateY((float) state.rotationAngle);
            state.itemEntity.setTransformation(transformation);
        }
    }

    private void spawn(PowerupType type, PowerupState state) {
        Location location = state.locations.get(random.nextInt(state.locations.size())).clone();
        World world = location.getWorld();
        if (world == null) return;

        ItemDisplay itemDisplay = world.spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(type.getMaterial()));
            d.setBillboard(Display.Billboard.FIXED);
            //d.setPersistent(false);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });

        NamedTextColor color = type == PowerupType.HEALTH ? NamedTextColor.GREEN : NamedTextColor.RED;
        String label = type == PowerupType.HEALTH ? "HEALING" : "DOUBLE DAMAGE";

        Location hologramLoc = location.clone().add(0, HOLOGRAM_HEIGHT_OFFSET, 0);
        TextDisplay hologram = world.spawn(hologramLoc, TextDisplay.class, d -> {
            d.text(Component.text(label, color));
            d.setBillboard(Display.Billboard.CENTER);
           // d.setPersistent(false);
            d.setSeeThrough(false);
            d.setShadowed(true);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        });

        EntityCleanupUtils.markAsArenaEntity(itemDisplay);
        EntityCleanupUtils.markAsArenaEntity(hologram);
        state.itemEntity = itemDisplay;
        state.hologramEntity = hologram;
        state.spawnedAt = location;
        state.rotationAngle = 0.0;

        NamedTextColor themeColor = type == PowerupType.HEALTH ? NamedTextColor.GREEN : NamedTextColor.RED;

        Component spawnMessage = Component.text("The ", NamedTextColor.YELLOW)
                .append(Component.text(type.getDisplayName(), themeColor, TextDecoration.BOLD))
                .append(Component.text(" has spawned!", NamedTextColor.YELLOW));

        for (Player player : world.getPlayers()) {
            player.sendMessage(spawnMessage);
        }
    }



    private void despawn(PowerupState state) {
        if (state.spawnedAt != null && state.spawnedAt.getWorld() != null) {
            if (!state.spawnedAt.getChunk().isLoaded()) {
                state.spawnedAt.getChunk().load();
            }
        }

        if (state.itemEntity != null) {
            state.itemEntity.remove();
            state.itemEntity = null;
        }
        if (state.hologramEntity != null) {
            state.hologramEntity.remove();
            state.hologramEntity = null;
        }
        state.spawnedAt = null;
    }
    public Map<PowerupType, Player> checkPickups(Collection<Player> players) {
        Map<PowerupType, Player> pickedUp = new EnumMap<>(PowerupType.class);

        for (Map.Entry<PowerupType, PowerupState> entry : states.entrySet()) {
            PowerupState state = entry.getValue();

            if (state.itemEntity == null || state.itemEntity.isDead() || state.spawnedAt == null) {
                continue;
            }

            for (Player player : players) {
                if (!player.isOnline()) continue;
                if (player.getWorld() != state.spawnedAt.getWorld()) continue;

                if (player.getLocation().distance(state.spawnedAt) <= PICKUP_RADIUS) {
                    pickedUp.put(entry.getKey(), player);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    markPickedUp(state);
                    despawn(state);
                    break;
                }
            }
        }

        return pickedUp;
    }
}