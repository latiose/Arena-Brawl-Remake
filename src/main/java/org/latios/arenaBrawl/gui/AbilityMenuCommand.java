// gui/AbilityMenuCommand.java
package org.latios.arenaBrawl.gui;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.game.MatchManager;

public class AbilityMenuCommand implements CommandExecutor {

    private final AbilitySelectorGUI gui;
    private final MatchManager matchManager;

    public AbilityMenuCommand(AbilitySelectorGUI gui, MatchManager matchManager) {
        this.gui = gui;
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (matchManager.isInMatch(player)) {
            player.sendMessage("§cYou can't change abilities while in a match.");
            return true;
        }

        gui.openMainMenu(player);
        return true;
    }
}