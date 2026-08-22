package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.latios.arenaBrawl.game.Match;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardManager {

    private final PlayerHealthManager healthManager;

    public ScoreboardManager(PlayerHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    /** Creates one individual Scoreboard per player in the match (needed for per-viewer nametag colors). */
    public Map<Player, Scoreboard> createIndividualScoreboards(Match match) {
        Map<Player, Scoreboard> boards = new HashMap<>();
        for (Player player : match.getAllPlayers()) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("health_display", "dummy", "§c❤ Health");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            boards.put(player, board);
            player.setScoreboard(board);
        }
        return boards;
    }

    public void updateHealthDisplay(Match match) {
        for (Map.Entry<Player, Scoreboard> entry : match.getIndividualScoreboards().entrySet()) {
            Scoreboard board = entry.getValue();
            Objective obj = board.getObjective("health_display");
            if (obj == null) continue;

            for (String scoreEntry : new java.util.HashSet<>(board.getEntries())) {
                board.resetScores(scoreEntry);
            }

            long elapsedSeconds = (System.currentTimeMillis() - match.getStartedAt()) / 1000;
            String timeStr = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);

            int line = 10;
            obj.getScore("§7Time: §f" + timeStr).setScore(line--);

            for (Player p : match.getRed()) {
                obj.getScore("§c" + p.getName() + ": " + (int) healthManager.getHealth(p) + " ❤").setScore(line--);
            }
            for (Player p : match.getBlue()) {
                obj.getScore("§9" + p.getName() + ": " + (int) healthManager.getHealth(p) + " ❤").setScore(line--);
            }
        }
    }
}