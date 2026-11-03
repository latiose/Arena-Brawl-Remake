package org.latios.arenaBrawl.lobby;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnHefneyCommand implements CommandExecutor {

    private final HefneyNPCManager hefneyNPCManager;

    public SpawnHefneyCommand(HefneyNPCManager hefneyNPCManager) {
        this.hefneyNPCManager = hefneyNPCManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can spawn Hefney NPCs.");
            return true;
        }

        hefneyNPCManager.spawn(player.getLocation());
        return true;
    }
}
