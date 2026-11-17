
package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.MatchManager;

public class HungerTask extends BukkitRunnable {

    private final HungerManager hungerManager;
    private final MatchManager matchManager;
    public HungerTask(HungerManager hungerManager,MatchManager matchManager) {
        this.hungerManager = hungerManager;
        this.matchManager = matchManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!matchManager.isInMatch(player)) continue;
            // HungerImmunity covers both Song of Power and Rune of Hunger.
            // HungerSuppression keeps the hunger removed by Rune of Starvation from regenerating.
            hungerManager.tick(player, HungerImmunity.isActive(player), HungerSuppression.isRegenBlocked(player));
        }
    }
}
