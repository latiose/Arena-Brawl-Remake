
package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CaptureStructureCommand implements CommandExecutor {

    private final Plugin plugin;

    public CaptureStructureCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§cOnly operators can use this command.");
            return true;
        }

        int radiusX = 3, radiusY = 3, radiusZ = 3;

        if (args.length >= 3) {
            try {
                radiusX = Integer.parseInt(args[0]);
                radiusY = Integer.parseInt(args[1]);
                radiusZ = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cUsage: /capturestructure [radiusX radiusY radiusZ]");
                return true;
            }
        }

        Location origin = player.getLocation().getBlock().getLocation(); // player's feet, block-snapped

        List<String> lines = new ArrayList<>();
        int count = 0;

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dy = 0; dy <= radiusY; dy++) { // dy starts at 0 (ground level and up), not negative
                for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                    Block block = origin.clone().add(dx, dy, dz).getBlock();
                    Material type = block.getType();

                    if (isIgnorable(type)) continue;

                    lines.add(String.format(
                            "offsets.add(new StructureBlueprint.BlockOffset(%d, %d, %d, Material.%s));",
                            dx, dy, dz, type.name()
                    ));
                    count++;
                }
            }
        }

        if (lines.isEmpty()) {
            player.sendMessage("§cNo non-air blocks found in that radius around you.");
            return true;
        }

        String fileContent = String.join("\n", lines);
        File outputFile = new File(plugin.getDataFolder(), "captured_structure.txt");

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(fileContent);
        } catch (IOException e) {
            player.sendMessage("§cFailed to write output file: " + e.getMessage());
            return true;
        }

        player.sendMessage("§a✔ Captured " + count + " blocks.");
        player.sendMessage("§7Saved to: §f" + outputFile.getPath());
        player.sendMessage("§7Preview (first 5 lines):");
        for (int i = 0; i < Math.min(5, lines.size()); i++) {
            player.sendMessage("§8" + lines.get(i));
        }
        if (lines.size() > 5) {
            player.sendMessage("§8... (" + (lines.size() - 5) + " more lines in the file)");
        }

        return true;
    }

    private boolean isIgnorable(Material type) {
        return type.isAir();
    }
}