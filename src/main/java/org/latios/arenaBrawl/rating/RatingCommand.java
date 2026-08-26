package org.latios.arenaBrawl.rating;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class RatingCommand implements CommandExecutor {

    private final RatingManager ratingManager;

    public RatingCommand(RatingManager ratingManager) {
        this.ratingManager = ratingManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length != 1) {
            player.sendMessage("§eYour rating is: §f" + (int) ratingManager.getRating(player));
        }
        else {
            Player target = Bukkit.getPlayerExact(args[0]);
            assert target != null;
            player.sendMessage("§eYour rating is: §f" + (int) ratingManager.getRating(target));
        }
        return true;
    }
}