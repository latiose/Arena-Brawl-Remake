package org.latios.arenaBrawl.abilities.config;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.latios.arenaBrawl.abilities.AbilityRegistry;
import org.latios.arenaBrawl.gui.AbilitySelectorGUI;
import org.latios.arenaBrawl.hats.HatConfigManager;
import org.latios.arenaBrawl.hats.HatSelectorGUI;
import org.latios.arenaBrawl.runes.RuneConfigManager;

public class ReloadAbilitiesCommand implements CommandExecutor {

    private final AbilityConfigManager configManager;
    private final AbilityRegistry abilityRegistry;
    private final AbilitySelectorGUI abilitySelectorGUI;
    private final HatConfigManager hatConfigManager;
    private final HatSelectorGUI hatSelectorGUI;
    private final RuneConfigManager runeConfigManager;
    public ReloadAbilitiesCommand(AbilityConfigManager configManager, AbilityRegistry abilityRegistry,
                                  AbilitySelectorGUI abilitySelectorGUI, HatConfigManager hatConfigManager,
                                  HatSelectorGUI hatSelectorGUI, RuneConfigManager runeConfigManager) {
        this.configManager = configManager;
        this.abilityRegistry = abilityRegistry;
        this.abilitySelectorGUI = abilitySelectorGUI;
        this.hatConfigManager = hatConfigManager;
        this.hatSelectorGUI = hatSelectorGUI;
        this.runeConfigManager = runeConfigManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        configManager.reload();
        abilityRegistry.clearPreviewCache();
        runeConfigManager.reload();
        hatConfigManager.reload();

        refreshOpenSelectorMenus();

        sender.sendMessage("§aAbilities, hats and runes reloaded! Open menus updated live.");
        return true;
    }

    /** Re-renders open menus for online players live. */
    private void refreshOpenSelectorMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory openInventory = player.getOpenInventory().getTopInventory();
            if (openInventory.getHolder() instanceof AbilitySelectorGUI.AbilitySelectorHolder holder) {
                if (holder.slot() != null) {
                    abilitySelectorGUI.openSlotMenu(player, holder.slot());
                } else if (!holder.isRuneMenu()) {
                    abilitySelectorGUI.openMainMenu(player);
                }
            }
            else if (openInventory.getHolder() instanceof HatSelectorGUI.HatSelectorHolder) {
                hatSelectorGUI.open(player);
            }
        }
    }
}