package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.latios.arenaBrawl.game.Game;

public class ScoreboardManager {

    private HealthUtils healthUtils;

    public ScoreboardManager(HealthUtils healthUtils) {
        this.healthUtils = healthUtils;
    }
    public Scoreboard createMatchScoreboard() {
        Scoreboard board = org.bukkit.Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("health_display", "dummy", "§c❤ Vida");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        return board;
    }

    public void assignToPlayers(Game game) {
        for (Player player : game.getAllPlayers()) {
            player.setScoreboard(game.getScoreboard());
        }
    }

    public void updateHealthDisplay(Game game) {
        Scoreboard board = game.getScoreboard();
        Objective obj = board.getObjective("health_display");
        if (obj == null) return;

        for (String entry : new java.util.HashSet<>(board.getEntries())) {
            board.resetScores(entry);
        }

        int line = 0;
        for (Player player : game.getRed()) {
            obj.getScore("§c" + player.getName() + ": " + (int) healthUtils.getHealth(player) + " HP")
                    .setScore(line--);
        }
        for (Player player : game.getBlue()) {
            obj.getScore("§9" + player.getName() + ": " + (int) healthUtils.getHealth(player) + " HP")
                    .setScore(line--);
        }
    }
}