package org.latios.arenaBrawl.general;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HealthUtils {

    private static final double VANILLA_MAX = 20.0;

    private final Map<UUID, Double> currentHealth = new HashMap<>();
    private final Map<UUID, Double> maxHealth = new HashMap<>();

    public void setMaxHealth(Player player, double max) {
        maxHealth.put(player.getUniqueId(), max);
        currentHealth.put(player.getUniqueId(), max);
        syncVanilla(player);
    }

    public double getMaxHealth(Player player) {
        return maxHealth.getOrDefault(player.getUniqueId(), 2000.0);
    }

    public double getHealth(Player player) {
        return currentHealth.getOrDefault(player.getUniqueId(), 2000.0);
    }

    public void heal(Player player, double amount) {
        double max = getMaxHealth(player);
        double updated = Math.min(getHealth(player) + amount, max);
        currentHealth.put(player.getUniqueId(), updated);
        syncVanilla(player);
    }

    public void damage(Player player, double amount) {
        double updated = Math.max(getHealth(player) - amount, 0);
        currentHealth.put(player.getUniqueId(), updated);
        syncVanilla(player);
    }

    public boolean isDead(Player player) {
        return getHealth(player) <= 0;
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

}