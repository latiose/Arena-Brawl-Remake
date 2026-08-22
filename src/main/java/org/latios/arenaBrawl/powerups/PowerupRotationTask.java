// powerups/PowerupRotationTask.java
package org.latios.arenaBrawl.powerups;

import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;

public class PowerupRotationTask extends BukkitRunnable {

    private final MatchManager matchManager;

    public PowerupRotationTask(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @Override
    public void run() {
        Match match = matchManager.getActiveMatch();
        if (match == null) return;

        match.getPowerupManager().tickRotation();
    }
}