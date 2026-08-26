
package org.latios.arenaBrawl.powerups;

import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;
import java.util.List;

public class PowerupRotationTask extends BukkitRunnable {

    private final MatchManager matchManager;

    public PowerupRotationTask(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @Override
    public void run() {
       List<Match> matches = matchManager.getActiveMatches();
        if (matches == null || matches.isEmpty()) return;
        for(Match match : matches) {
            match.getPowerupManager().tickRotation();
        }
    }
}