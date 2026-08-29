package org.latios.arenaBrawl.general;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.latios.arenaBrawl.ArenaBrawlPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class PlayerHealthManager {

    private static final double VANILLA_MAX = 20.0;
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, Double> currentHealth = new HashMap<>();
    private final Map<UUID, Double> maxHealth = new HashMap<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private static final long HOLOGRAM_LIFETIME_TICKS = 35;
    private Consumer<Player> eliminationCallback = p -> {};

    public void setEliminationCallback(Consumer<Player> callback) {
        this.eliminationCallback = callback;
    }

    public void setMaxHealth(Player player, double max) {
        maxHealth.put(player.getUniqueId(), max);
        currentHealth.put(player.getUniqueId(), max);
        eliminated.remove(player.getUniqueId());
        syncVanilla(player);
    }

    public double getMaxHealth(Player player) {
        return maxHealth.getOrDefault(player.getUniqueId(), 2000.0);
    }

    public double getHealth(Player player) {
        return currentHealth.getOrDefault(player.getUniqueId(), 2000.0);
    }

    public void heal(Player player, double amount) {
        if (eliminated.contains(player.getUniqueId())) return;
        double max = getMaxHealth(player);
        double updated = Math.min(getHealth(player) + amount, max);
        currentHealth.put(player.getUniqueId(), updated);
        spawnHologram(player, String.valueOf(amount), player.getLocation());
        syncVanilla(player);
    }

    public void damage(Player player, double amount, Player attacker) {
        if (eliminated.contains(player.getUniqueId())) return;

        if (attacker != null) {
            lastAttacker.put(player.getUniqueId(), attacker.getUniqueId());
        }

        double updated = Math.max(getHealth(player) - amount, 0);
        currentHealth.put(player.getUniqueId(), updated);
        syncVanilla(player);

        if (updated <= 0) {
            eliminated.add(player.getUniqueId());
            eliminationCallback.accept(player);
        }
    }

    public boolean isEliminated(Player player) {
        return eliminated.contains(player.getUniqueId());
    }

    private void syncVanilla(Player player) {
        double percentage = getHealth(player) / getMaxHealth(player);
        double vanillaHealth = Math.max(0.5, percentage * VANILLA_MAX);

        var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null && attribute.getBaseValue() != VANILLA_MAX) {
            attribute.setBaseValue(VANILLA_MAX);
        }

        player.setHealth(Math.min(vanillaHealth, VANILLA_MAX));
    }

    public void damage(Player player, double amount) {
        damage(player, amount, null);
    }

    public UUID getLastAttacker(Player player) {
        return lastAttacker.get(player.getUniqueId());
    }

    public void damageSilent(Player player, double amount, Player attacker) {
        if (eliminated.contains(player.getUniqueId())) return;

        if (attacker != null) {
            lastAttacker.put(player.getUniqueId(), attacker.getUniqueId());
        }

        double updated = Math.max(getHealth(player) - amount, 0);
        currentHealth.put(player.getUniqueId(), updated);

        if (updated <= 0) {
            eliminated.add(player.getUniqueId());
            eliminationCallback.accept(player);
        }
    }

    public void damageSilent(Player player, double amount) {
        damageSilent(player, amount, null);
    }

    private void spawnHologram(Player victim, String phrase, Location impactLocation) {
        float jitterX = (float) ThreadLocalRandom.current().nextDouble(-0.15, 0.15);
        float jitterY = (float) ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
        float jitterZ = (float) ThreadLocalRandom.current().nextDouble(-0.15, 0.15);

        Location spawnLoc = impactLocation.clone().add(0, 1.0, 0);
        TextColor color = TextColor.color(0x90EE90);

        TextDisplay hologram = victim.getWorld().spawn(spawnLoc, TextDisplay.class, d -> {
            d.text(Component.text(phrase, color));
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(true);
            d.setShadowed(true);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(100, 10, 50, 20));

            Transformation currentTransform = d.getTransformation();
            Vector3f translation = new Vector3f(jitterX, jitterY, jitterZ);

            d.setTransformation(new Transformation(
                    translation,
                    currentTransform.getLeftRotation(),
                    currentTransform.getScale(),
                    currentTransform.getRightRotation()
            ));
        });
        EntityCleanupUtils.markAsArenaEntity(hologram);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (hologram.isDead()) {
                    cancel();
                    return;
                }

                if (ticksElapsed >= HOLOGRAM_LIFETIME_TICKS) {
                    hologram.remove();
                    cancel();
                    return;
                }
                hologram.teleport(hologram.getLocation().add(0, 0.02, 0));
                ticksElapsed++;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
    }
}