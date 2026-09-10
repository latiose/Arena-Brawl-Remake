package org.latios.arenaBrawl.rating;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.lobby.LobbyScoreboardManager;

public class SetRatingCommand implements CommandExecutor {

    private final RatingManager ratingManager;
    private final LobbyScoreboardManager lobbyScoreboardManager;
    public SetRatingCommand(RatingManager ratingManager,LobbyScoreboardManager lobbyScoreboardManager) {
        this.ratingManager = ratingManager;
        this.lobbyScoreboardManager = lobbyScoreboardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("§cUsage: /setrating <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or not online.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a number.");
            return true;
        }

        ratingManager.setRating(target, amount);
        lobbyScoreboardManager.update(target);
        return true;
    }
}