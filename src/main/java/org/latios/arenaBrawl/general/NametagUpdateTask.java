
package org.latios.arenaBrawl.general;

import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;

public class NametagUpdateTask extends BukkitRunnable {

    private final MatchManager matchManager;
    private final NametagManager nametagManager;

    public NametagUpdateTask(MatchManager matchManager, NametagManager nametagManager) {
        this.matchManager = matchManager;
        this.nametagManager = nametagManager;
    }

    @Override
    public void run() {
        for (Match match : matchManager.getActiveMatches()) {
            nametagManager.update(match);
        }
    }
}