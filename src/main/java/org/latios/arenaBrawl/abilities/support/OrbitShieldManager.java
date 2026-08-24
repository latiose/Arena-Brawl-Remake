
package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.latios.arenaBrawl.general.EntityCleanupUtils;

import java.util.*;

public class OrbitShieldManager {

    private static final double ORBIT_RADIUS = 1.3;
    private static final double ORBIT_HEIGHT_OFFSET = 1.4;
    private static final double ROTATION_SPEED_PER_TICK = 0.05;

    public int getCharges(Player player) {
        ShieldState state = activeShields.get(player.getUniqueId());
        return state.charges.toArray().length;
    }

    private static class ShieldState {
        OrbitShieldType type;
        final List<Entity> charges = new ArrayList<>();
        long startedAt;
        double rotationOffset = 0.0;
        int tickCounter = 0;
    }

    private final Map<UUID, ShieldState> activeShields = new HashMap<>();

    public void activate(Player player, OrbitShieldType type) {
        clear(player);

        ShieldState state = new ShieldState();
        state.type = type;
        state.startedAt = System.currentTimeMillis();

        for (int i = 0; i < type.getChargeCount(); i++) {
            state.charges.add(spawnCharge(player, type));
        }

        activeShields.put(player.getUniqueId(), state);
    }

    private Entity spawnCharge(Player player, OrbitShieldType type) {
        return switch (type.getVisualType()) {
            case ITEM_DISPLAY -> spawnItemDisplay(player, type.getMaterial());
            case CHARGED_CREEPER -> spawnChargedCreeper(player);
        };
    }

    private ItemDisplay spawnItemDisplay(Player player, org.bukkit.Material material) {
        ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(material));
            d.setBillboard(Display.Billboard.CENTER);

            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(0.55f, 0.55f, 0.55f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });
        EntityCleanupUtils.markAsArenaEntity(display);
        return display;
    }

    private Creeper spawnChargedCreeper(Player player) {
        Creeper creeper = player.getWorld().spawn(player.getLocation(), Creeper.class, c -> {
            c.setPowered(true);
            c.setInvisible(true);
            c.setSilent(true);
            c.setAI(false);
            c.setInvulnerable(true);
            c.setCollidable(false);
            c.setGravity(false);
            c.setPersistent(false);
        });
        EntityCleanupUtils.markAsArenaEntity(creeper);
        org.latios.arenaBrawl.general.CollisionUtils.disableCollision(creeper, player.getScoreboard());
        return creeper;
    }

    public boolean hasActiveShield(Player player) {
        return activeShields.containsKey(player.getUniqueId());
    }

    public OrbitShieldType getActiveType(Player player) {
        ShieldState state = activeShields.get(player.getUniqueId());
        return state != null ? state.type : null;
    }

    public OrbitShieldType consumeCharge(Player player) {
        ShieldState state = activeShields.get(player.getUniqueId());
        if (state == null || state.charges.isEmpty()) return null;

        Entity charge = state.charges.remove(state.charges.size() - 1);
        charge.remove();

        OrbitShieldType type = state.type;

        if (state.charges.isEmpty()) {
            activeShields.remove(player.getUniqueId());
        }

        return type;
    }

    public void clear(Player player) {
        OrbitShieldType type = getActiveType(player);
        ShieldState state = activeShields.remove(player.getUniqueId());
        if (state == null) return;

        if(!(type.getVisualType() == OrbitShieldVisualType.CHARGED_CREEPER)) {
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_SKELETON_DEATH, 1.0f, 1.0f);
        }
        despawnAll(state);
    }

    public Set<UUID> getActiveShieldPlayers() {
        return activeShields.keySet();
    }

    /** Called once per real tick. Internally decides per-shield whether it's this shield's turn to update. */
    public void tickOrbits(Server server) {
        var iterator = activeShields.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            ShieldState state = entry.getValue();
            Player player = server.getPlayer(entry.getKey());

            if (player == null || !player.isOnline()) {
                despawnAll(state);
                iterator.remove();
                continue;
            }

            long elapsed = System.currentTimeMillis() - state.startedAt;
            if (elapsed >= state.type.getDurationMillis()) {
                despawnAll(state);
                iterator.remove();
                continue;
            }

            state.tickCounter++;

            // Always advance the rotation math every real tick, so the angle stays
            // consistent regardless of how often the entity position is actually pushed.
            state.rotationOffset += ROTATION_SPEED_PER_TICK;

            // Only move the entities on this shield type's configured interval.
            if (state.tickCounter % state.type.getUpdateIntervalTicks() != 0) {
                continue;
            }

            int count = state.charges.size();
            for (int i = 0; i < count; i++) {
                double angle = state.rotationOffset + (2 * Math.PI * i / count);
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;

                Entity charge = state.charges.get(i);

                if (state.type.getVisualType() == OrbitShieldVisualType.CHARGED_CREEPER) {
                    Location target = player.getLocation().clone().add(x, 0, z);
                    charge.teleport(target);
                } else {
                    // Posición objetivo absoluta en este tick
                    Location targetLoc = player.getLocation().clone().add(x, ORBIT_HEIGHT_OFFSET, z);

                    // Posición actual de la entidad
                    Location currentLoc = charge.getLocation();

                    // Si cambia de mundo o está demasiado lejos, teletransporta directo
                    if (currentLoc.getWorld() != targetLoc.getWorld() || currentLoc.distanceSquared(targetLoc) > 16.0) {
                        charge.teleport(targetLoc);
                    } else {
                        // INTERPOLACIÓN MANUAL (LERP):
                        // 0.355 es el factor de suavizado (0.1 = más delay, 0.9 = más rígido)
                        double factor = 0.35;

                        double lerpX = currentLoc.getX() + (targetLoc.getX() - currentLoc.getX()) * factor;
                        double lerpy = currentLoc.getY() + (targetLoc.getY() - currentLoc.getY()) * factor;
                        double lerpZ = currentLoc.getZ() + (targetLoc.getZ() - currentLoc.getZ()) * factor;

                        Location finalLoc = new Location(player.getWorld(), lerpX, lerpy, lerpZ, 0f, 0f);
                        charge.teleport(finalLoc);

                    }
                }
            }
        }
    }

    private void despawnAll(ShieldState state) {
        for (Entity charge : state.charges) {
            if (!charge.isDead()) charge.remove();
        }
    }
}