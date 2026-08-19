package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnergyManager {

    public static final double MAX_ENERGY = 100.0;
    public static final double REGEN_PER_SECOND = 2.0;

    private final Map<UUID, Double> energy = new HashMap<>();

    public double getEnergy(Player player) {
        return energy.getOrDefault(player.getUniqueId(), 0.0);
    }

    public boolean hasEnough(Player player, double amount) {
        return getEnergy(player) >= amount;
    }

    public void consume(Player player, double amount) {
        double current = getEnergy(player);
        double updated = Math.max(0, current - amount);
        energy.put(player.getUniqueId(), updated);
        syncVisual(player);
    }

    public void regenTick(Player player) {
        double current = getEnergy(player);
        if (current >= MAX_ENERGY) return;

        double updated = Math.min(MAX_ENERGY, current + REGEN_PER_SECOND);
        energy.put(player.getUniqueId(), updated);
        syncVisual(player);
    }

    public void reset(Player player) {
        energy.put(player.getUniqueId(), 0.0);
        syncVisual(player);
    }

    private void syncVisual(Player player) {
        double current = getEnergy(player);
        player.setLevel((int) current);
        player.setExp((float) (current / MAX_ENERGY));
    }
}