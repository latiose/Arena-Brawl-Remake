
package org.latios.arenaBrawl.lobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.latios.arenaBrawl.rating.RatingManager;
import org.latios.arenaBrawl.stats.PlayerStats;
import org.latios.arenaBrawl.stats.StatsManager;

public class LobbyScoreboardManager {

    private final RatingManager ratingManager;
    private final StatsManager statsManager;

    public LobbyScoreboardManager(RatingManager ratingManager, StatsManager statsManager) {
        this.ratingManager = ratingManager;
        this.statsManager = statsManager;
    }

    public void show(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("lobby_stats", "dummy", "§6§lArenaBrawl");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerStats stats = statsManager.getStats(player);
        int rating = (int) ratingManager.getRating(player);

        obj.getScore("§fRating: §e" + rating).setScore(4);
        obj.getScore("§fWins: §a" + stats.wins).setScore(3);
        obj.getScore("§fKills: §c" + stats.kills).setScore(2);
        obj.getScore("§fCoins: §6" + stats.coins).setScore(1);

        player.setScoreboard(board);
    }

    public void update(Player player){
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("lobby_stats", "dummy", "§6§lArenaBrawl");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerStats stats = statsManager.getStats(player);
        int rating = (int) ratingManager.getRating(player);

        obj.getScore("§fRating: §e" + rating).setScore(4);
        obj.getScore("§fWins: §a" + stats.wins).setScore(3);
        obj.getScore("§fKills: §c" + stats.kills).setScore(2);
        obj.getScore("§fCoins: §6" + stats.coins).setScore(1);

        player.setScoreboard(board);
    }
}