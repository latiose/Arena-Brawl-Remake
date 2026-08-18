package org.latios.arenaBrawl.general;

import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.Match;

public class MatchScoreboardTask extends BukkitRunnable {

    private final Match match;
    private final ScoreboardManager scoreboardManager;

    public MatchScoreboardTask(Match match, ScoreboardManager scoreboardManager) {
        this.match = match;
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public void run() {
        scoreboardManager.updateHealthDisplay(match);
    }
}