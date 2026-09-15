package org.latios.arenaBrawl.general;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ToggleBlockBreakCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenabrawl.admin.togglebreak")) {
            sender.sendMessage(Component.text("You do not have permission to execute this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("enable")) {
                BlockBreakListener.setBreakProtectionEnabled(true);
            } else if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("disable")) {
                BlockBreakListener.setBreakProtectionEnabled(false);
            } else {
                sender.sendMessage(Component.text("Usage: /toggleblockbreak [on|off]", NamedTextColor.RED));
                return true;
            }
        } else {
            boolean currentState = BlockBreakListener.isBreakProtectionEnabled();
            BlockBreakListener.setBreakProtectionEnabled(!currentState);
        }

        boolean newState = BlockBreakListener.isBreakProtectionEnabled();
        if (newState) {
            sender.sendMessage(Component.text("Block breaking protection is now ENABLED.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Block breaking protection is now DISABLED.", NamedTextColor.YELLOW));
        }

        return true;
    }
}