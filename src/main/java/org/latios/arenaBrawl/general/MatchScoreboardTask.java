package org.latios.arenaBrawl.general;

import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.Game;

public class MatchScoreboardTask extends BukkitRunnable {

    private final Game game;
    private final ScoreboardManager scoreboardManager;

    public MatchScoreboardTask(Game game, ScoreboardManager scoreboardManager) {
        this.game = game;
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public void run() {
        scoreboardManager.updateHealthDisplay(game);
    }
}