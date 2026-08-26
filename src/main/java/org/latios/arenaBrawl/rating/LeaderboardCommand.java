// rating/LeaderboardCommand.java
package org.latios.arenaBrawl.rating;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public class LeaderboardCommand implements CommandExecutor {

    private static final int TOP_SIZE = 10;

    private final RatingManager ratingManager;

    public LeaderboardCommand(RatingManager ratingManager) {
        this.ratingManager = ratingManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        List<Map.Entry<String, Double>> top = ratingManager.getTopRatings(TOP_SIZE);

        if (top.isEmpty()) {
            sender.sendMessage("§eThe leaderboard is empty!.");
            return true;
        }

        sender.sendMessage("§6§l== Top " + TOP_SIZE + " Rating ==");
        int position = 1;
        for (Map.Entry<String, Double> entry : top) {
            sender.sendMessage("§7#" + position + " §f" + entry.getKey() + " §7- §e" + Math.round(entry.getValue()));
            position++;
        }

        return true;
    }
}