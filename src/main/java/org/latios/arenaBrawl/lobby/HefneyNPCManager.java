package org.latios.arenaBrawl.lobby;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.latios.arenaBrawl.general.AbilityItemKeys;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HefneyNPCManager implements Listener {

    private static final double HOLOGRAM_HEIGHT_OFFSET = 2.6;
    private static final double GAZE_RADIUS = 12.0;

    private final Plugin plugin;
    private final ArenaChallengerGUI gui;
    private final Map<UUID, TextDisplay> holograms = new HashMap<>();

    public HefneyNPCManager(Plugin plugin, ArenaChallengerGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void spawn(Location base) {
        final Location anchor = base.getBlock().getLocation().add(0.5, 0.0, 0.5);
        anchor.setYaw(base.getYaw());

        Husk npc = anchor.getWorld().spawn(anchor, Husk.class, z -> {
            configureNpc(z);
            z.getPersistentDataContainer().set(
                    AbilityItemKeys.HEFNEY_NPC,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        });

        applyDisguise(npc);
        attachHologram(npc);
    }

    /**
     * Drops tracked hologram state for every NPC in the given world, used
     * before /killhefney removes them.
     */
    public void notifyNpcsAboutToBeRemoved(World world) {
        Iterator<Map.Entry<UUID, TextDisplay>> iterator = holograms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TextDisplay> entry = iterator.next();
            Entity npc = Bukkit.getEntity(entry.getKey());

            if (npc != null && npc.getWorld().equals(world)) {
                TextDisplay display = entry.getValue();
                if (display != null && !display.isDead()) {
                    display.remove();
                }
                iterator.remove();
            }
        }
    }

    private void configureNpc(Zombie npc) {
        npc.setAI(false);
        npc.setSilent(true);
        npc.setInvulnerable(true);
        npc.setCanPickupItems(false);
        npc.setCollidable(false);
        npc.setShouldBurnInDay(false);
        npc.customName(null);
        npc.setRemoveWhenFarAway(false);
    }

    private static final UUID SKIN_UUID = UUID.fromString("11a5192a-284c-4051-a4cd-960a8f4c7e83");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private volatile UserProfile cachedProfile;
    private volatile boolean fetching = false;
    private final Set<UUID> pendingNpcs = ConcurrentHashMap.newKeySet();

    private void applyDisguise(Zombie npc) {
        if (DisguiseAPI.isDisguised(npc)) return;

        if (cachedProfile != null) {
            disguiseNow(npc);
            return;
        }

        pendingNpcs.add(npc.getUniqueId());
        fetchProfile();
    }

    private void fetchProfile() {
        if (fetching || cachedProfile != null) return;
        fetching = true;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/"
                        + SKIN_UUID.toString().replace("-", "") + "?unsigned=false"))
                .GET()
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        fetching = false;
                        return;
                    }

                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray props = json.getAsJsonArray("properties");
                    List<TextureProperty> textures = new java.util.ArrayList<>();
                    for (var el : props) {
                        JsonObject p = el.getAsJsonObject();
                        textures.add(new TextureProperty(
                                p.get("name").getAsString(),
                                p.get("value").getAsString(),
                                p.has("signature") ? p.get("signature").getAsString() : null
                        ));
                    }

                    cachedProfile = new UserProfile(SKIN_UUID, json.get("name").getAsString(), textures);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (UUID id : pendingNpcs) {
                            Entity e = Bukkit.getEntity(id);
                            if (e instanceof Zombie z && z.isValid()) disguiseNow(z);
                        }
                        pendingNpcs.clear();
                    });
                })
                .exceptionally(error -> {
                    fetching = false;
                    return null;
                });
    }

    private void disguiseNow(Zombie npc) {
        if (DisguiseAPI.isDisguised(npc)) return;

        PlayerDisguise disguise = new PlayerDisguise("Hefney");
        disguise.setSkin(cachedProfile);
        disguise.setNameVisible(false);
        disguise.setViewSelfDisguise(false);
        DisguiseAPI.disguiseToAll(npc, disguise);
    }
    private void tick() {
        Iterator<Map.Entry<UUID, TextDisplay>> iterator = holograms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TextDisplay> entry = iterator.next();
            TextDisplay display = entry.getValue();
            Entity npc = Bukkit.getEntity(entry.getKey());

            if (!(npc instanceof Zombie) || !npc.isValid() || npc.isDead()
                    || display == null || display.isDead()) {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
                iterator.remove();
                continue;
            }

            Player lookTarget = findClosestPlayer(npc);
            if (lookTarget != null) {
                face(npc, lookTarget);
            }

            Location holoTarget = npc.getLocation().add(0, HOLOGRAM_HEIGHT_OFFSET, 0);
            if (display.getLocation().distanceSquared(holoTarget) > 0.01) {
                display.teleport(holoTarget);
            }
        }

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof Zombie zombie && isHefneyNpc(zombie)) {
                        configureNpc(zombie);
                        applyDisguise(zombie);
                        attachHologram(zombie);
                    }
                }
            }
        }
    }

    private Player findClosestPlayer(Entity npc) {
        Player best = null;
        double bestDist = GAZE_RADIUS * GAZE_RADIUS;

        Location eye = npc.getLocation().add(0, 1.7, 0);
        for (Player candidate : eye.getWorld().getNearbyEntitiesByType(Player.class, eye, GAZE_RADIUS)) {
            if (candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;

            double dist = eye.distanceSquared(candidate.getEyeLocation());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private void face(Entity npc, Player target) {
        Location npcLoc = npc.getLocation();
        Vector delta = target.getEyeLocation().toVector()
                .subtract(npcLoc.toVector().add(new Vector(0, 1.7, 0)));
        double horizontal = Math.sqrt(delta.getX() * delta.getX() + delta.getZ() * delta.getZ());
        if (horizontal < 0.001) return;

        float yaw = (float) Math.toDegrees(Math.atan2(-delta.getX(), delta.getZ()));
        float pitch = (float) Math.toDegrees(-Math.atan2(delta.getY(), horizontal));

        float dYaw = Math.abs(((yaw - npcLoc.getYaw() + 540) % 360) - 180);
        if (dYaw < 2f && Math.abs(pitch - npcLoc.getPitch()) < 2f) return;

        npcLoc.setYaw(yaw);
        npcLoc.setPitch(pitch);
        npc.teleport(npcLoc);

        if (npc instanceof LivingEntity living) {
            living.setBodyYaw(yaw);
        }
    }

    private boolean isHefneyNpc(Entity entity) {
        return entity.getPersistentDataContainer().has(AbilityItemKeys.HEFNEY_NPC, PersistentDataType.BYTE);
    }

    private void attachHologram(Zombie npc) {
        UUID npcId = npc.getUniqueId();
        TextDisplay existing = holograms.get(npcId);
        if (existing != null && existing.isValid() && !existing.isDead()) {
            return;
        }

        Location holoLoc = npc.getLocation().add(0, HOLOGRAM_HEIGHT_OFFSET, 0);
        for (Entity nearby : holoLoc.getWorld().getNearbyEntities(holoLoc, 0.5, 0.5, 0.5)) {
            if (nearby instanceof TextDisplay textDisplay
                    && textDisplay.getPersistentDataContainer().has(
                    AbilityItemKeys.HEFNEY_HOLOGRAM, PersistentDataType.BYTE)) {
                holograms.put(npcId, textDisplay);
                return;
            }
        }

        World world = holoLoc.getWorld();
        if (world == null) return;

        TextDisplay display = world.spawn(holoLoc, TextDisplay.class, d -> {
            d.text(Component.text("The Sand Merchant", NamedTextColor.YELLOW, TextDecoration.BOLD));
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
                    AbilityItemKeys.HEFNEY_HOLOGRAM, PersistentDataType.BYTE, (byte) 1
            );
            d.setPersistent(true);
        });

        holograms.put(npcId, display);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Zombie npc)) return;
        if (!isHefneyNpc(npc)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        gui.open(player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Zombie npc)) return;
        if (!isHefneyNpc(npc)) return;

        event.setCancelled(true);
    }
}
