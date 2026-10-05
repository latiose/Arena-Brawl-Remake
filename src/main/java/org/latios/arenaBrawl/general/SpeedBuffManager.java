package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpeedBuffManager {

    private static class SpeedBuff {
        private final int amplifier;
        private final long expireTimeMillis;

        public SpeedBuff(int amplifier, long durationMillis) {
            this.amplifier = amplifier;
            this.expireTimeMillis = System.currentTimeMillis() + durationMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expireTimeMillis;
        }

        public int getAmplifier() {
            return amplifier;
        }

        public long getExpireTimeMillis() {
            return expireTimeMillis;
        }

        public boolean dominates(SpeedBuff other) {
            return amplifier >= other.amplifier && expireTimeMillis >= other.expireTimeMillis;
        }
    }

    private final Plugin plugin;
    private final Map<UUID, List<SpeedBuff>> activeBuffs = new ConcurrentHashMap<>();

    public SpeedBuffManager(Plugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    public void applyBuff(Player player, int amplifier, long durationMillis) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        SpeedBuff incoming = new SpeedBuff(amplifier, durationMillis);

        List<SpeedBuff> buffs = activeBuffs.computeIfAbsent(uuid, k -> new CopyOnWriteArrayList<>());
        buffs.removeIf(SpeedBuff::isExpired);

        for (SpeedBuff existing : buffs) {
            if (existing.dominates(incoming)) {
                updatePlayerSpeed(player);
                return;
            }
        }

        buffs.removeIf(incoming::dominates);
        buffs.add(incoming);

        updatePlayerSpeed(player);
    }

    public void updatePlayerSpeed(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        List<SpeedBuff> buffs = activeBuffs.get(uuid);

        if (buffs == null) {
            applyBaseSpeed(player);
            return;
        }

        buffs.removeIf(SpeedBuff::isExpired);

        if (buffs.isEmpty()) {
            activeBuffs.remove(uuid, buffs);
            applyBaseSpeed(player);
            return;
        }

        int maxAmplifier = Integer.MIN_VALUE;
        for (SpeedBuff buff : buffs) {
            maxAmplifier = Math.max(maxAmplifier, buff.getAmplifier());
        }

        long latestExpiration = 0L;
        for (SpeedBuff buff : buffs) {
            if (buff.getAmplifier() == maxAmplifier) {
                latestExpiration = Math.max(latestExpiration, buff.getExpireTimeMillis());
            }
        }

        long remainingMs = Math.max(1L, latestExpiration - System.currentTimeMillis());
        int durationTicks = (int) Math.max(1L, (remainingMs + 49L) / 50L);

        PotionEffect existing = player.getPotionEffect(PotionEffectType.SPEED);

        if (existing != null
                && existing.getAmplifier() == maxAmplifier
                && existing.getDuration() >= durationTicks - 1) {
            return;
        }

        player.removePotionEffect(PotionEffectType.SPEED);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                durationTicks,
                maxAmplifier,
                true,
                false
        ));
    }

    private void applyBaseSpeed(Player player) {
        PotionEffect existing = player.getPotionEffect(PotionEffectType.SPEED);

        if (existing != null
                && existing.getAmplifier() == 0
                && existing.getDuration() > 40) {
            return;
        }

        player.removePotionEffect(PotionEffectType.SPEED);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                PotionEffect.INFINITE_DURATION,
                0,
                true,
                false
        ));
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeBuffs.keySet()) {
                    Player player = plugin.getServer().getPlayer(uuid);

                    if (player != null && player.isOnline()) {
                        updatePlayerSpeed(player);
                    } else {
                        activeBuffs.remove(uuid);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 2L);
    }

    public void clearBuffs(Player player) {
        if (player == null) return;

        activeBuffs.remove(player.getUniqueId());

        applyBaseSpeed(player);
    }
}