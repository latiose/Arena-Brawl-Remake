package org.latios.arenaBrawl.game;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HubCommand implements CommandExecutor {

    private final MatchManager matchManager;

    public HubCommand(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!matchManager.isInMatch(player)) {
            player.sendMessage("§cYou are not in a match");
            return true;
        }

        matchManager.leaveMatch(player);
        return true;
    }
}