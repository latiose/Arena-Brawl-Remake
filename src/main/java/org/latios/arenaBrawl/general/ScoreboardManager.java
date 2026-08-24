package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.latios.arenaBrawl.game.Match;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class ScoreboardManager {

    private final PlayerHealthManager healthManager;

    public ScoreboardManager(PlayerHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    public Map<Player, Scoreboard> createIndividualScoreboards(Match match) {
        Map<Player, Scoreboard> boards = new HashMap<>();
        for (Player player : match.getAllPlayers()) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

            Objective obj = board.registerNewObjective("arena_brawl", "dummy", "§b§lArena Brawl");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            boards.put(player, board);
            player.setScoreboard(board);
        }
        return boards;
    }

    public void updateHealthDisplay(Match match) {
        long elapsedSeconds = (System.currentTimeMillis() - match.getStartedAt()) / 1000;
        String timeStr = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);

        for (Map.Entry<Player, Scoreboard> entry : match.getIndividualScoreboards().entrySet()) {
            Player viewer = entry.getKey();
            Scoreboard board = entry.getValue();
            Objective obj = board.getObjective("arena_brawl");
            if (obj == null) continue;

            obj.setDisplayName("§f§lArena Brawl §b" + timeStr);

            for (String scoreEntry : new java.util.HashSet<>(board.getEntries())) {
                board.resetScores(scoreEntry);
            }

            boolean isViewerRed = match.getRed().contains(viewer);
            List<Player> myTeam = isViewerRed ? match.getRed() : match.getBlue();
            List<Player> enemyTeam = isViewerRed ? match.getBlue() : match.getRed();

            int line = 8;

            obj.getScore(" ").setScore(line--);

            obj.getScore("§c[ENEMY TEAM]").setScore(line--);
            for (Player enemy : enemyTeam) {
                int health = (int) healthManager.getHealth(enemy);
                if(health != 0) {
                    obj.getScore("§7" + enemy.getName() + " §e" + health).setScore(line--);
                }
                else{
                    obj.getScore("§7" + enemy.getName() + " §e" + "DEAD").setScore(line--);
                }
            }

            obj.getScore("  ").setScore(line--);

            obj.getScore("§a[YOUR TEAM]").setScore(line--);
            for (Player teammate : myTeam) {
                int health = (int) healthManager.getHealth(teammate);
                if(health != 0) {
                    obj.getScore("§7" + teammate.getName() + " §e" + health).setScore(line--);
                }
                else{
                    obj.getScore("§7" + teammate.getName() + " §e" + "DEAD").setScore(line--);
                }
            }
        }
    }
}