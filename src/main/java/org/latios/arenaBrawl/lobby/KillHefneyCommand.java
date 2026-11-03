package org.latios.arenaBrawl.lobby;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;

/**
 * Kills every villager and every zombie (Hefney NPCs included) in the
 * dimension the command sender currently is in.
 */
public class KillHefneyCommand implements CommandExecutor {

    private final HefneyNPCManager hefneyNPCManager;

    public KillHefneyCommand(HefneyNPCManager hefneyNPCManager) {
        this.hefneyNPCManager = hefneyNPCManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage("§cOnly players can run this from a dimension.");
            return true;
        }

        World world = player.getWorld();
        int count = 0;

        // Let the NPC manager clean its tracked state first so hologram
        // bookkeeping matches reality on the next tick.
        hefneyNPCManager.notifyNpcsAboutToBeRemoved(world);

        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            villager.remove();
            count++;
        }

        for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
            zombie.remove();
            count++;
        }

        sender.sendMessage(String.format("§aKilled %d entities (villagers and zombies) in %s.", count, world.getName()));
        return true;
    }
}
