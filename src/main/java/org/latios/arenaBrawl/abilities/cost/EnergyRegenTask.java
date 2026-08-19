package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.EnergyManager;

public class EnergyRegenTask extends BukkitRunnable {

    private final EnergyManager energyManager;
    private final MatchManager matchManager;
    public EnergyRegenTask(EnergyManager energyManager,MatchManager matchManager) {
        this.energyManager = energyManager;
        this.matchManager = matchManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!matchManager.isInMatch(player)) continue;
            energyManager.regenTick(player);
        }
    }
}