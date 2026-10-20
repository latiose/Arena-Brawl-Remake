package org.latios.arenaBrawl.abilities.utility;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Shadow Cloak's invisibility and the hidden armor/held items.
 *
 * <p>Minecraft's invisibility effect does not hide worn armor or held items.
 * While the cloak is active the entity id is registered here so
 * {@code InvisibilityPacketListener} can blank every outgoing equipment packet;
 * the real inventory is never touched, so abilities and melee keep working. A
 * fresh equipment packet is sent when the cloak ends to reveal everything
 * again.</p>
 *
 * <p>The damage bonus is applied through {@link DamageBuffManager} as a
 * single-use buff: it empowers the next melee hit or ability and is consumed
 * by that hit, which also ends the cloak immediately.</p>
 */
public class ShadowCloakManager implements Listener {

    private static final String BUFF_TITLE = "Shadow Cloak";
    private static final String SPEED_BUFF_SOURCE = "Shadow Cloak";

    private static final class ActiveCloak {
        private BukkitTask restoreTask;
    }

    private final Map<UUID, ActiveCloak> active = new HashMap<>();
    private final Set<Integer> cloakedEntityIds = ConcurrentHashMap.newKeySet();
    private final Plugin plugin;
    private final SpeedBuffManager speedBuffManager;
    private final DamageBuffManager damageBuffManager;

    public ShadowCloakManager(Plugin plugin, SpeedBuffManager speedBuffManager,
                              DamageBuffManager damageBuffManager) {
        this.plugin = plugin;
        this.speedBuffManager = speedBuffManager;
        this.damageBuffManager = damageBuffManager;
    }

    public void activate(Player player, long durationMs, double bonusMultiplier, int speedAmplifier) {
        restore(player);

        int durationTicks = Math.max(1, (int) Math.ceil(durationMs / 50.0));

        ActiveCloak cloak = new ActiveCloak();
        active.put(player.getUniqueId(), cloak);
        cloakedEntityIds.add(player.getEntityId());

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY, durationTicks, 0, false, false, false));

        if (speedBuffManager != null) {
            speedBuffManager.applyBuff(player, speedAmplifier, durationMs, SPEED_BUFF_SOURCE);
        }
        if (damageBuffManager != null) {
            damageBuffManager.applySingleUseBuff(
                    player, Math.max(1.0, bonusMultiplier), durationMs, BUFF_TITLE,
                    () -> restore(player)
            );
        }

        player.getWorld().spawnParticle(
                Particle.SMOKE, player.getLocation().add(0, 1, 0), 30,
                0.5, 0.8, 0.5, 0.02
        );
        player.getWorld().playSound(
                player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f
        );

        // Re-send the real equipment so the packet listener replaces it with
        // empty slots for every viewer, including players already tracking us.
        resendEquipment(player);

        cloak.restoreTask = Bukkit.getScheduler().runTaskLater(
                plugin, () -> restore(player), durationTicks
        );
    }

    private void resendEquipment(Player player) {
        PlayerInventory inv = player.getInventory();
        List<Equipment> list = List.of(
                new Equipment(EquipmentSlot.MAIN_HAND, SpigotConversionUtil.fromBukkitItemStack(inv.getItemInMainHand())),
                new Equipment(EquipmentSlot.OFF_HAND, SpigotConversionUtil.fromBukkitItemStack(inv.getItemInOffHand())),
                new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(inv.getHelmet())),
                new Equipment(EquipmentSlot.CHEST_PLATE, SpigotConversionUtil.fromBukkitItemStack(inv.getChestplate())),
                new Equipment(EquipmentSlot.LEGGINGS, SpigotConversionUtil.fromBukkitItemStack(inv.getLeggings())),
                new Equipment(EquipmentSlot.BOOTS, SpigotConversionUtil.fromBukkitItemStack(inv.getBoots()))
        );

        for (Player viewer : player.getWorld().getPlayers()) {
            if (viewer.equals(player)) continue;
            WrapperPlayServerEntityEquipment packet =
                    new WrapperPlayServerEntityEquipment(player.getEntityId(), list);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }

    public boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public boolean isCloakedEntity(int entityId) {
        return cloakedEntityIds.contains(entityId);
    }

    public void clear(Player player) {
        restore(player);
    }

    public void clearAll() {
        for (UUID id : active.keySet().toArray(UUID[]::new)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                restore(player);
            } else {
                active.remove(id);
            }
        }
        cloakedEntityIds.clear();
    }

    private void restore(Player player) {
        ActiveCloak cloak = active.remove(player.getUniqueId());
        if (cloak == null) return;

        if (cloak.restoreTask != null) {
            cloak.restoreTask.cancel();
        }

        cloakedEntityIds.remove(player.getEntityId());

        if (player.isOnline() && !player.isDead()) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.updateInventory();
            resendEquipment(player);
        }

        if (damageBuffManager != null) {
            damageBuffManager.removeBuff(player, BUFF_TITLE);
        }
        if (speedBuffManager != null) {
            speedBuffManager.removeBuff(player, SPEED_BUFF_SOURCE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        restore(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ActiveCloak cloak = active.remove(player.getUniqueId());
        if (cloak != null && cloak.restoreTask != null) {
            cloak.restoreTask.cancel();
        }
        cloakedEntityIds.remove(player.getEntityId());
        if (damageBuffManager != null) {
            damageBuffManager.removeBuff(player, BUFF_TITLE);
        }
        if (speedBuffManager != null) {
            speedBuffManager.removeBuff(player, SPEED_BUFF_SOURCE);
        }
    }

}
