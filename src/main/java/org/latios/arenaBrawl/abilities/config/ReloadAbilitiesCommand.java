
package org.latios.arenaBrawl.abilities.config;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadAbilitiesCommand implements CommandExecutor {

    private final AbilityConfigManager configManager;

    public ReloadAbilitiesCommand(AbilityConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        configManager.reload();
        sender.sendMessage("§aAbility config reloaded. Changes apply to the next match started "
                + "(current matches keep their already-built ability instances).");
        return true;
    }
}