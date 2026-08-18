// general/HungerTask.java
package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HungerTask extends BukkitRunnable {

    private final HungerManager hungerManager;

    public HungerTask(HungerManager hungerManager) {
        this.hungerManager = hungerManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            hungerManager.tick(player);
        }
    }
}