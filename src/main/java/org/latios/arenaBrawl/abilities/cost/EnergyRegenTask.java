package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.general.EnergyManager;

public class EnergyRegenTask extends BukkitRunnable {

    private final EnergyManager energyManager;

    public EnergyRegenTask(EnergyManager energyManager) {
        this.energyManager = energyManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            energyManager.regenTick(player);
        }
    }
}