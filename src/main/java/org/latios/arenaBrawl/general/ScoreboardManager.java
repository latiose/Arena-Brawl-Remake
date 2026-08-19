package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.latios.arenaBrawl.game.Match;

public class ScoreboardManager {

    private PlayerHealthManager playerHealthManager;

    public ScoreboardManager(PlayerHealthManager playerHealthManager) {
        this.playerHealthManager = playerHealthManager;
    }
    public Scoreboard createMatchScoreboard() {
        Scoreboard board = org.bukkit.Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("health_display", "dummy", "§c❤ HP");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        return board;
    }

    public void assignToPlayers(Match match) {
        for (Player player : match.getAllPlayers()) {
            player.setScoreboard(match.getScoreboard());
        }
    }

    public void updateHealthDisplay(Match match) {
        Scoreboard board = match.getScoreboard();
        Objective obj = board.getObjective("health_display");
        if (obj == null) return;

        for (String entry : new java.util.HashSet<>(board.getEntries())) {
            board.resetScores(entry);
        }

        int line = 0;
        for (Player player : match.getRed()) {
            obj.getScore("§c" + player.getName() + ": " + (int) playerHealthManager.getHealth(player) + " HP")
                    .setScore(line--);
        }
        for (Player player : match.getBlue()) {
            obj.getScore("§9" + player.getName() + ": " + (int) playerHealthManager.getHealth(player) + " HP")
                    .setScore(line--);
        }
    }
}