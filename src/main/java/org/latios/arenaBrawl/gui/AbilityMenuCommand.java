// gui/AbilityMenuCommand.java
package org.latios.arenaBrawl.gui;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AbilityMenuCommand implements CommandExecutor {

    private final AbilitySelectorGUI gui;

    public AbilityMenuCommand(AbilitySelectorGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        gui.openMainMenu(player);
        return true;
    }
}