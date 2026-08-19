// debuffs/DebuffTickTask.java
package org.latios.arenaBrawl.debuffs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DebuffTickTask extends BukkitRunnable {

    private final DebuffManager debuffManager;

    public DebuffTickTask(DebuffManager debuffManager) {
        this.debuffManager = debuffManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            debuffManager.tick(player);
        }
    }
}