package org.latios.arenaBrawl.stats;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.lobby.LobbyScoreboardManager;

public class SetCoinGainCommand implements CommandExecutor {

    private final StatsManager statsManager;

    public SetCoinGainCommand(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /setcoingain <multiplier>");
            return true;
        }


        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a whole number.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cAmount must be greater than 0.");
            return true;
        }

        statsManager.setMultiplier(amount);

        return true;
    }
}