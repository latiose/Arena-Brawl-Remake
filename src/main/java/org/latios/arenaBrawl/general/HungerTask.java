
package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.support.SongOfPowerManager;
import org.latios.arenaBrawl.game.MatchManager;

public class HungerTask extends BukkitRunnable {

    private final HungerManager hungerManager;
    private final MatchManager matchManager;
    private final SongOfPowerManager songOfPowerManager;
    public HungerTask(HungerManager hungerManager,MatchManager matchManager,SongOfPowerManager songOfPowerManager) {
        this.hungerManager = hungerManager;
        this.matchManager = matchManager;
        this.songOfPowerManager = songOfPowerManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!matchManager.isInMatch(player)) continue;
            hungerManager.tick(player, songOfPowerManager.isActive(player));
        }
    }
}