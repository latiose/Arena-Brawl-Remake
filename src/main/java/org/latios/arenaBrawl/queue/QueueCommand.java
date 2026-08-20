// queue/QueueCommand.java
package org.latios.arenaBrawl.queue;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.game.MatchManager;

public class QueueCommand implements CommandExecutor {

    private final QueueManager queueManager;
    private final MatchManager matchManager;

    public QueueCommand(QueueManager queueManager, MatchManager matchManager) {
        this.queueManager = queueManager;
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (matchManager.isInMatch(player)) {
            player.sendMessage("§cYou can't use the queue while in a match.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /queue <join|leave>");
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            if (queueManager.isQueued(player)) {
                player.sendMessage("§cYou are already in the queue.");
                return true;
            }
            boolean joined = queueManager.joinQueue(player);
            if (joined) {
                player.sendMessage("§aYou joined the queue (" + queueManager.getQueueSize() + "/4).");
            } else {
                player.sendMessage("§cA member of your party is already in queue.");
            }
        } else if (args[0].equalsIgnoreCase("leave")) {
            queueManager.leaveQueue(player);
            player.sendMessage("§eYou left the queue.");
        }

        return true;
    }
}