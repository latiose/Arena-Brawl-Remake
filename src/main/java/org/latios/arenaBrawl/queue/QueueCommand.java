// queue/QueueCommand.java
package org.latios.arenaBrawl.queue;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QueueCommand implements CommandExecutor {

    private final QueueManager queueManager;

    public QueueCommand(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players allowed.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUse: /queue <join|leave>");
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            if (queueManager.isQueued(player)) {
                player.sendMessage("§cYou are already in a queue.");
                return true;
            }
            boolean joined = queueManager.joinQueue(player);
            if (joined) {
                player.sendMessage("§aYou have joined the queue (" + queueManager.getQueueSize() + "/4).");
            } else {
                player.sendMessage("§cA party member is already in a queue.");
            }
        } else if (args[0].equalsIgnoreCase("leave")) {
            queueManager.leaveQueue(player);
            player.sendMessage("§eYou have left the queue.");
        }

        return true;
    }
}