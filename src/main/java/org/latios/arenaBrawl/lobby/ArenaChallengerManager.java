
package org.latios.arenaBrawl.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.latios.arenaBrawl.general.AbilityItemKeys;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


public class ArenaChallengerManager implements Listener {

    private static final double HOLOGRAM_HEIGHT_OFFSET = 2.6;

    private final Plugin plugin;
    private final ArenaChallengerGUI gui;
    private final Map<UUID, TextDisplay> holograms = new HashMap<>();

    public ArenaChallengerManager(Plugin plugin, ArenaChallengerGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickHolograms, 40L, 20L);
    }

    private void tickHolograms() {
        Iterator<Map.Entry<UUID, TextDisplay>> iterator = holograms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TextDisplay> entry = iterator.next();
            TextDisplay display = entry.getValue();
            Entity villager = Bukkit.getEntity(entry.getKey());

            if (!(villager instanceof Villager) || !villager.isValid() || villager.isDead()
                    || display == null || display.isDead()) {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
                iterator.remove();
                continue;
            }

            Location target = villager.getLocation().add(0, HOLOGRAM_HEIGHT_OFFSET, 0);
            if (!display.getLocation().equals(target)) {
                display.teleport(target);
            }
        }

        // Attach holograms to any villager without one.
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof Villager villager) {
                        configureNpc(villager);
                        attachHologram(villager);
                    }
                }
            }
        }
    }

    /** Turns the villager into a static Arena Challenger NPC. */
    private void configureNpc(Villager villager) {
        villager.setAI(false);
        villager.setCanPickupItems(false);
        villager.setSilent(true);
    }

    private void attachHologram(Villager villager) {
        UUID villagerId = villager.getUniqueId();
        TextDisplay existing = holograms.get(villagerId);
        if (existing != null && existing.isValid() && !existing.isDead()) {
            return;
        }

        // Re-adopt a hologram if one was left behind by a previous runtime.
        Location holoLoc = villager.getLocation().add(0, HOLOGRAM_HEIGHT_OFFSET, 0);
        for (Entity nearby : holoLoc.getWorld().getNearbyEntities(holoLoc, 0.5, 0.5, 0.5)) {
            if (nearby instanceof TextDisplay textDisplay
                    && textDisplay.getPersistentDataContainer().has(
                    AbilityItemKeys.ARENA_CHALLENGER_HOLOGRAM, PersistentDataType.BYTE)) {
                holograms.put(villagerId, textDisplay);
                return;
            }
        }

        World world = holoLoc.getWorld();
        if (world == null) return;

        TextDisplay display = world.spawn(holoLoc, TextDisplay.class, d -> {
            d.text(Component.text("ARENA CHALLENGER", NamedTextColor.YELLOW, TextDecoration.BOLD));
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(false);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new AxisAngle4f()
            ));
            d.setViewRange(64.0f);
            d.getPersistentDataContainer().set(
                    AbilityItemKeys.ARENA_CHALLENGER_HOLOGRAM, PersistentDataType.BYTE, (byte) 1
            );
            d.setPersistent(true);
        });

        holograms.put(villagerId, display);
    }

    @EventHandler
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Villager)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        gui.open(player);
    }

    /** Arena Challenger NPCs are invulnerable. */
    @EventHandler
    public void onVillagerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;

        event.setCancelled(true);
    }
}
