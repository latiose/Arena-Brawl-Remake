package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.EnergyManager;

public class EnergyRegenTask extends BukkitRunnable {

    private final EnergyManager energyManager;
    private final MatchManager matchManager;
    private final EnergyModifierManager energyModifierManager;

    public EnergyRegenTask(EnergyManager energyManager, MatchManager matchManager, EnergyModifierManager energyModifierManager) {
        this.energyManager = energyManager;
        this.matchManager = matchManager;
        this.energyModifierManager = energyModifierManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!matchManager.isInMatch(player)) continue;
            double multiplier = energyModifierManager.getTotalMultiplier(player);
            energyManager.regenTick(player, multiplier);
        }
    }
}