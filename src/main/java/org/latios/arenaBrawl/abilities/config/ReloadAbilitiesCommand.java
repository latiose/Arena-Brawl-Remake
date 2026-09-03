package org.latios.arenaBrawl.abilities.config;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.latios.arenaBrawl.abilities.AbilityRegistry;
import org.latios.arenaBrawl.gui.AbilitySelectorGUI;

public class ReloadAbilitiesCommand implements CommandExecutor {

    private final AbilityConfigManager configManager;
    private final AbilityRegistry abilityRegistry;
    private final AbilitySelectorGUI abilitySelectorGUI;

    public ReloadAbilitiesCommand(AbilityConfigManager configManager, AbilityRegistry abilityRegistry,
                                  AbilitySelectorGUI abilitySelectorGUI) {
        this.configManager = configManager;
        this.abilityRegistry = abilityRegistry;
        this.abilitySelectorGUI = abilitySelectorGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        configManager.reload();
        abilityRegistry.clearPreviewCache();
        refreshOpenSelectorMenus();

        sender.sendMessage("§aAbility config reloaded. New values apply to menus immediately, "
                + "and to the next match started (matches already in progress keep their current values).");
        return true;
    }

    /** Force-refreshes any ability selector menu a player currently has open, so they see updated numbers live. */
    private void refreshOpenSelectorMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory openInventory = player.getOpenInventory().getTopInventory();

            if (openInventory.getHolder() instanceof AbilitySelectorGUI.AbilitySelectorHolder holder) {
                if (holder.slot() != null) {
                    abilitySelectorGUI.openSlotMenu(player, holder.slot()); // re-renders with fresh values
                } else if (holder.isRuneMenu()) {
                    continue;
                } else {
                    abilitySelectorGUI.openMainMenu(player);
                }
            }
        }
    }
}