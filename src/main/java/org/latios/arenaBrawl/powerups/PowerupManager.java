package org.latios.arenaBrawl.powerups;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.EntityCleanupUtils;

import java.util.*;

public class PowerupManager {

    private static final double PICKUP_RADIUS = 1.8;
    private static final float ITEM_SCALE = 0.4f;
    private static final double ROTATION_SPEED_PER_TICK = 0.1;
    private static final double HOLOGRAM_HEIGHT_OFFSET = 0.6;

    private static class PowerupState {
        List<Location> locations = List.of();
        ItemDisplay itemEntity;
        TextDisplay hologramEntity;
        ItemDisplay energyItemEntity;
        TextDisplay energyHologramEntity;
        Location spawnedAt;
        double rotationAngle = 0.0;
        long lastHandledCycleStart = -1;
        boolean pickedUpThisWindow = false;
        long spawnTimeInCycle = -1;
        /**
         * Whether each viewer is currently seeing the energy variant. Keyed by
         * the Player instance rather than the UUID so a player who reconnects
         * (a new entity/connection) is synced again instead of seeing both
         * variants stacked on top of each other.
         */
        final Map<Player, Boolean> viewingEnergy = new IdentityHashMap<>();
    }

    private final Map<PowerupType, PowerupState> states = new EnumMap<>(PowerupType.class);
    private final Random random = new Random();
    private final PowerupPreferenceManager preferenceManager;

    public PowerupManager() {
        this(ArenaBrawlPlugin.getInstance() != null
                ? ArenaBrawlPlugin.getInstance().getPowerupPreferenceManager()
                : null);
    }

    public PowerupManager(PowerupPreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
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
            state.spawnTimeInCycle = -1;
        }
    }

    public void clear() {
        reset();
    }

    /** Call once per second with the match's elapsed time in millis. */
    public void tick(long matchElapsedMillis) {
        for (PowerupType type : PowerupType.values()) {
            PowerupState state = states.get(type);
            if (state.locations.isEmpty()) continue;

            PowerupSchedule schedule = type.getSchedule();
            long cycleLength = schedule.cycleMillis();
            long cyclePosition = matchElapsedMillis % cycleLength;
            long currentCycleStart = matchElapsedMillis - cyclePosition;

            if (currentCycleStart != state.lastHandledCycleStart) {
                state.lastHandledCycleStart = currentCycleStart;
                state.pickedUpThisWindow = false;

                long windowStart = schedule.windowStartMillis();
                long windowEnd = schedule.windowEndMillis();
                long windowDuration = windowEnd - windowStart;

                if (windowDuration > 0) {
                    state.spawnTimeInCycle = windowStart + (long) (random.nextDouble() * windowDuration);
                } else {
                    state.spawnTimeInCycle = windowStart;
                }
            }

            boolean entityStillAlive = isAlive(state.itemEntity) || isAlive(state.energyItemEntity);

            if (!entityStillAlive && !state.pickedUpThisWindow && state.spawnTimeInCycle != -1) {
                if (cyclePosition >= state.spawnTimeInCycle && schedule.isWithinWindow(cyclePosition)) {
                    spawn(type, state);
                }
            }
        }
    }

    private void markPickedUp(PowerupState state) {
        state.pickedUpThisWindow = true;
    }

    public void tickRotation() {
        for (Map.Entry<PowerupType, PowerupState> entry : states.entrySet()) {
            PowerupType type = entry.getKey();
            PowerupState state = entry.getValue();

            boolean hasDisplay = isAlive(state.itemEntity) || isAlive(state.energyItemEntity);
            if (!hasDisplay) continue;

            state.rotationAngle += ROTATION_SPEED_PER_TICK;
            applyRotation(state.itemEntity, state.rotationAngle);
            applyRotation(state.energyItemEntity, state.rotationAngle);

            if (state.spawnedAt != null) {
                syncVisibility(type, state);
            }
        }
    }

    private boolean isAlive(ItemDisplay display) {
        return display != null && !display.isDead();
    }

    private void applyRotation(ItemDisplay display, double angle) {
        if (!isAlive(display)) return;

        Transformation transformation = display.getTransformation();
        transformation.getLeftRotation().identity().rotateY((float) angle);
        display.setTransformation(transformation);
    }

    private void spawn(PowerupType type, PowerupState state) {
        Location location = state.locations.get(random.nextInt(state.locations.size())).clone();
        World world = location.getWorld();
        if (world == null) return;

        state.itemEntity = null;
        state.hologramEntity = null;
        state.energyItemEntity = null;
        state.energyHologramEntity = null;
        state.viewingEnergy.clear();

        spawnDisplay(type, location, state, false);

        // The damage powerup has a personal energy variant that shares the
        // same spawn point, schedule and pickup: only the visuals change.
        if (type == PowerupType.DAMAGE) {
            spawnDisplay(PowerupType.ENERGY, location, state, true);
        }

        state.spawnedAt = location;
        state.rotationAngle = 0.0;

        sendSpawnMessage(type, world);
        scheduleInitialVisibility(type, state);
    }

    private void spawnDisplay(PowerupType visualType, Location location, PowerupState state,
                              boolean energyVariant) {
        World world = location.getWorld();
        if (world == null) return;

        ItemDisplay itemDisplay = world.spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(visualType.getMaterial()));
            d.setBillboard(Display.Billboard.FIXED);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });

        Location hologramLoc = location.clone().add(0, HOLOGRAM_HEIGHT_OFFSET, 0);
        TextDisplay hologram = world.spawn(hologramLoc, TextDisplay.class, d -> {
            d.text(Component.text(visualType.getLabel(), visualType.getColor()));
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(false);
            d.setShadowed(true);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        });

        EntityCleanupUtils.markAsArenaEntity(itemDisplay);
        EntityCleanupUtils.markAsArenaEntity(hologram);

        if (energyVariant) {
            state.energyItemEntity = itemDisplay;
            state.energyHologramEntity = hologram;
        } else {
            state.itemEntity = itemDisplay;
            state.hologramEntity = hologram;
        }
    }

    private void sendSpawnMessage(PowerupType type, World world) {
        for (Player player : world.getPlayers()) {
            PowerupType visual = getVisualType(type, player);
            Component spawnMessage = Component.text("The ", NamedTextColor.YELLOW)
                    .append(Component.text(visual.getDisplayName(), visual.getColor(), TextDecoration.BOLD))
                    .append(Component.text(" has spawned!", NamedTextColor.YELLOW));
            player.sendMessage(spawnMessage);
        }
    }

    /**
     * The powerup each player should see/hear about. Only the damage powerup
     * has a personal energy variant; every other type is unchanged.
     */
    public PowerupType getVisualType(PowerupType type, Player player) {
        if (type == PowerupType.DAMAGE && preferenceManager != null
                && preferenceManager.usesEnergyPowerup(player)) {
            return PowerupType.ENERGY;
        }
        return type;
    }

    /**
     * Entity tracking sends spawn packets during the world tick, which may
     * happen after the first visibility sync. Re-applying once shortly after
     * spawning makes sure the hidden variant never stays visible.
     */
    private void scheduleInitialVisibility(PowerupType type, PowerupState state) {
        if (type != PowerupType.DAMAGE) return;

        Plugin plugin = ArenaBrawlPlugin.getInstance();
        if (plugin == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state.spawnedAt == null || state.energyItemEntity == null) return;
            state.viewingEnergy.clear();
            syncVisibility(type, state);
        }, 2L);
    }

    private void syncVisibility(PowerupType type, PowerupState state) {
        if (type != PowerupType.DAMAGE) return;
        if (state.energyItemEntity == null || state.energyHologramEntity == null) return;

        World world = state.spawnedAt != null ? state.spawnedAt.getWorld() : null;
        if (world == null) return;

        List<Player> viewers = world.getPlayers();
        // Drop entries for players who left or changed worlds.
        state.viewingEnergy.keySet().removeIf(player -> !player.isOnline() || !viewers.contains(player));

        for (Player player : viewers) {
            boolean energy = preferenceManager != null && preferenceManager.usesEnergyPowerup(player);
            Boolean current = state.viewingEnergy.get(player);
            if (current != null && current == energy) continue;

            applyView(state, player, energy);
            state.viewingEnergy.put(player, energy);
        }
    }

    private void applyView(PowerupState state, Player player, boolean energy) {
        Plugin plugin = ArenaBrawlPlugin.getInstance();
        if (plugin == null) return;

        if (energy) {
            hide(player, plugin, state.itemEntity);
            hide(player, plugin, state.hologramEntity);
            show(player, plugin, state.energyItemEntity);
            show(player, plugin, state.energyHologramEntity);
        } else {
            hide(player, plugin, state.energyItemEntity);
            hide(player, plugin, state.energyHologramEntity);
            show(player, plugin, state.itemEntity);
            show(player, plugin, state.hologramEntity);
        }
    }

    private void hide(Player player, Plugin plugin, Entity entity) {
        if (entity != null) {
            player.hideEntity(plugin, entity);
        }
    }

    private void show(Player player, Plugin plugin, Entity entity) {
        if (entity != null) {
            player.showEntity(plugin, entity);
        }
    }

    private void despawn(PowerupState state) {
        if (state.spawnedAt != null && state.spawnedAt.getWorld() != null) {
            if (!state.spawnedAt.getChunk().isLoaded()) {
                state.spawnedAt.getChunk().load();
            }
        }

        remove(state.itemEntity);
        state.itemEntity = null;
        remove(state.hologramEntity);
        state.hologramEntity = null;
        remove(state.energyItemEntity);
        state.energyItemEntity = null;
        remove(state.energyHologramEntity);
        state.energyHologramEntity = null;
        state.viewingEnergy.clear();
        state.spawnedAt = null;
    }

    private void remove(Entity entity) {
        if (entity != null) {
            entity.remove();
        }
    }

    public Map<PowerupType, Player> checkPickups(Collection<Player> players) {
        Map<PowerupType, Player> pickedUp = new EnumMap<>(PowerupType.class);

        for (Map.Entry<PowerupType, PowerupState> entry : states.entrySet()) {
            PowerupState state = entry.getValue();

            if (state.spawnedAt == null) {
                continue;
            }
            if (!isAlive(state.itemEntity) && !isAlive(state.energyItemEntity)) {
                continue;
            }

            // Collect everyone standing in the pickup radius, then pick one at
            // random: iterating the player collection directly always favoured
            // the first accounts in it whenever several players camped the spawn.
            List<Player> candidates = new ArrayList<>();
            for (Player player : players) {
                if (!player.isOnline()) continue;
                if (player.getGameMode().equals(GameMode.SPECTATOR)) continue;
                if (player.getWorld() != state.spawnedAt.getWorld()) continue;

                if (player.getLocation().distance(state.spawnedAt) <= PICKUP_RADIUS) {
                    candidates.add(player);
                }
            }

            if (candidates.isEmpty()) continue;

            Player winner = candidates.get(random.nextInt(candidates.size()));
            pickedUp.put(entry.getKey(), winner);
            winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            markPickedUp(state);
            despawn(state);
        }

        return pickedUp;
    }
}
