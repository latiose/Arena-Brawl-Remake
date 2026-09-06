package org.latios.arenaBrawl.game;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DrawCommand implements CommandExecutor {

    private final MatchManager matchManager;
    private final DrawVoteManager drawVoteManager;

    public DrawCommand(MatchManager matchManager, DrawVoteManager drawVoteManager) {
        this.matchManager = matchManager;
        this.drawVoteManager = drawVoteManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Match match = matchManager.getMatchFor(player);
        if (match == null) {
            player.sendMessage("§cYou can only vote for a draw while in a match.");
            return true;
        }

        if (match.isEnded()) {
            return true; // match already ending, ignore
        }

        DrawVoteManager.VoteResult result = drawVoteManager.registerVote(player, match);
        int totalPlayers = match.getAllPlayers().size();

        switch (result) {
            case ALREADY_VOTED -> player.sendMessage("§cYou already voted for a draw.");

            case RECORDED -> {
                int votesNow = drawVoteManager.getVoteCount(match);
                broadcastToMatch(match, String.format(
                        "§b%s §bvoted to draw the match! §b(%d/%d)", player.getName(), votesNow, totalPlayers
                ));
            }

            case UNANIMOUS -> {
                broadcastToMatch(match, String.format(
                        "§b%s §bvoted to draw the match! §b(%d/%d)", player.getName(), totalPlayers, totalPlayers
                ));
                broadcastToMatch(match, "§bAll players agreed, the match will end in a draw!");
                matchManager.endMatchAsDraw(match);
            }
        }

        return true;
    }

    private void broadcastToMatch(Match match, String message) {
        for (Player viewer : match.getAllPlayers()) {
            if (viewer.isOnline()) {
                viewer.sendMessage(message);
            }
        }
    }
}