package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BaseSpeedTask extends BukkitRunnable {

    private final SpeedBuffManager speedBuffManager;

    public BaseSpeedTask(SpeedBuffManager speedBuffManager) {
        this.speedBuffManager = speedBuffManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            speedBuffManager.updatePlayerSpeed(player);
        }
    }
}