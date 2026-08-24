package org.latios.arenaBrawl.hats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.general.EntityCleanupUtils;

import java.util.HashMap;
import java.util.Map;

public class MagicalChestHologramListener implements Listener {

    private final Plugin plugin;
    private final Map<Location, TextDisplay> holograms = new HashMap<>();

    public MagicalChestHologramListener(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadExistingEnderChests();
    }

    private void loadExistingEnderChests() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                scanChunkForEnderChests(chunk);
            }
        }
    }

    private void scanChunkForEnderChests(Chunk chunk) {
        for (BlockState tile : chunk.getTileEntities()) {
            if (tile.getType() == Material.ENDER_CHEST) {
                spawnHologram(tile.getLocation());
            }
        }
    }

    private void spawnHologram(Location chestLoc) {
        if (holograms.containsKey(chestLoc)) return;

        Location holoLoc = chestLoc.clone().add(0.5, 1.3, 0.5);
        World world = chestLoc.getWorld();
        if (world == null) return;

        TextDisplay display = world.spawn(holoLoc, TextDisplay.class, d -> {
            Component text = Component.text("Magical Chest", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text("CLICK ME", NamedTextColor.YELLOW, TextDecoration.BOLD));

            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(false);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        });

        EntityCleanupUtils.markAsArenaEntity(display);
        holograms.put(chestLoc, display);
    }

    private void removeHologram(Location chestLoc) {
        TextDisplay display = holograms.remove(chestLoc);
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    public void removeAllHolograms() {
        for (TextDisplay display : holograms.values()) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        holograms.clear();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        scanChunkForEnderChests(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (BlockState tile : event.getChunk().getTileEntities()) {
            if (tile.getType() == Material.ENDER_CHEST) {
                removeHologram(tile.getLocation());
            }
        }
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