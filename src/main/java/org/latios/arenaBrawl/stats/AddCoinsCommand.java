package org.latios.arenaBrawl.stats;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddCoinsCommand implements CommandExecutor {

    private final StatsManager statsManager;

    public AddCoinsCommand(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("§cUsage: /addcoins <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or not online.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a whole number.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cAmount must be greater than 0.");
            return true;
        }

        statsManager.addCoins(target, amount);

        sender.sendMessage(String.format("§aAdded %d coins to %s.", amount, target.getName()));

        return true;
    }
}