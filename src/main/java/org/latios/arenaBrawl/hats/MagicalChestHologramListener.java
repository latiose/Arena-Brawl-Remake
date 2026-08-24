package org.latios.arenaBrawl.hats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import org.joml.Vector3f;
import org.latios.arenaBrawl.general.EntityCleanupUtils;

import java.util.HashMap;
import java.util.Map;

public class MagicalChestHologramListener implements Listener {

    private final Plugin plugin;
    private final Map<Location, TextDisplay> holograms = new HashMap<>();

    public MagicalChestHologramListener(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskLater(plugin, this::scanAllWorlds, 60L);
    }

    public void scanAllWorlds() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                scanChunk(chunk);
            }
        }
    }

    private void scanChunk(Chunk chunk) {
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        World world = chunk.getWorld();

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.ENDER_CHEST) {
                        spawnHologram(block.getLocation());
                    }
                }
            }
        }
    }

    private void spawnHologram(Location chestLoc) {
        if (holograms.containsKey(chestLoc)) return;

        Location holoLoc = chestLoc.clone().add(0.5, 1.25, 0.5);
        World world = chestLoc.getWorld();
        if (world == null) return;


        for (Entity nearby : world.getNearbyEntities(holoLoc, 0.5, 0.5, 0.5)) {
            if (nearby instanceof TextDisplay) {
                nearby.remove();
            }
        }

        TextDisplay display = world.spawn(holoLoc, TextDisplay.class, d -> {
            Component text = Component.text("Magical Chest", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text("CLICK ME", NamedTextColor.YELLOW, TextDecoration.BOLD));

            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(false);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            d.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(0, 0, 0),
                    new org.joml.AxisAngle4f(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new org.joml.AxisAngle4f()
            ));
            d.setViewRange(64.0f);
        });

       // EntityCleanupUtils.markAsArenaEntity(display);
        holograms.put(chestLoc, display);
    }

    private void removeHologram(Location chestLoc) {
        TextDisplay display = holograms.remove(chestLoc);
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }


    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> scanChunk(event.getChunk()));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.ENDER_CHEST) {
            spawnHologram(event.getBlockPlaced().getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.ENDER_CHEST) {
            removeHologram(event.getBlock().getLocation());
        }
    }
}