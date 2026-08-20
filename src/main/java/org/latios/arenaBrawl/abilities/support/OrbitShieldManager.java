// abilities/support/OrbitShieldManager.java
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

import java.util.*;

public class OrbitShieldManager {

    private static final double ORBIT_RADIUS = 1.3;
    private static final double ORBIT_HEIGHT_OFFSET = 0.3;
    private static final double ROTATION_SPEED_PER_TICK = 0.05;

    private static class ShieldState {
        OrbitShieldType type;
        final List<Entity> charges = new ArrayList<>();
        long startedAt;
        double rotationOffset = 0.0;
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
        return player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(material));
            d.setBillboard(Display.Billboard.CENTER);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(0.6f, 0.6f, 0.6f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });
    }

    private Creeper spawnChargedCreeper(Player player) {
        return player.getWorld().spawn(player.getLocation(), Creeper.class, c -> {
            c.setPowered(true);
            c.setInvisible(true);
            c.setSilent(true);
            c.setAI(false);           // prevents swelling/exploding entirely, no goals run
            c.setInvulnerable(true);
            c.setCollidable(false);
            c.setGravity(false);
            c.setPersistent(false);
        });
    }

    public boolean hasActiveShield(Player player) {
        return activeShields.containsKey(player.getUniqueId());
    }

    public OrbitShieldType getActiveType(Player player) {
        ShieldState state = activeShields.get(player.getUniqueId());
        return state != null ? state.type : null;
    }

    /** Consumes one charge. Returns the OrbitShieldType consumed, or null if no shield was active. */
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
        ShieldState state = activeShields.remove(player.getUniqueId());
        if (state == null) return;
        despawnAll(state);
    }

    public Set<UUID> getActiveShieldPlayers() {
        return activeShields.keySet();
    }

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

            state.rotationOffset += ROTATION_SPEED_PER_TICK;
            int count = state.charges.size();

            for (int i = 0; i < count; i++) {
                double angle = state.rotationOffset + (2 * Math.PI * i / count);
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;

                Location target = player.getLocation().clone().add(x, ORBIT_HEIGHT_OFFSET, z);
                state.charges.get(i).teleport(target);
            }
        }
    }

    private void despawnAll(ShieldState state) {
        for (Entity charge : state.charges) {
            if (!charge.isDead()) charge.remove();
        }
    }
}