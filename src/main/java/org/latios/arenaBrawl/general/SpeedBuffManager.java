
package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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


        public int getLevels() {
            return amplifier + 1;
        }

        public long getExpireTimeMillis() {
            return expireTimeMillis;
        }

        public long getRemainingTicks() {
            long remainingMs = expireTimeMillis - System.currentTimeMillis();
            return Math.max(1L, (remainingMs + 49L) / 50L);
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

        List<SpeedBuff> buffs = activeBuffs.computeIfAbsent(
                uuid,
                k -> Collections.synchronizedList(new ArrayList<>())
        );

        buffs.add(new SpeedBuff(amplifier, durationMillis));

        updatePlayerSpeed(player);
    }

    public void updatePlayerSpeed(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        List<SpeedBuff> buffs = activeBuffs.get(uuid);

        if (buffs == null || buffs.isEmpty()) {
            applyBaseSpeed(player);
            activeBuffs.remove(uuid);
            return;
        }

        synchronized (buffs) {
            buffs.removeIf(SpeedBuff::isExpired);
        }

        if (buffs.isEmpty()) {
            applyBaseSpeed(player);
            activeBuffs.remove(uuid);
            return;
        }

        List<SpeedBuff> sortedBuffs;

        synchronized (buffs) {
            sortedBuffs = new ArrayList<>(buffs);
        }

        sortedBuffs.sort(
                Comparator.comparingLong(SpeedBuff::getExpireTimeMillis)
        );

        long now = System.currentTimeMillis();


        int totalLevels = 0;
        long earliestExpiration = Long.MAX_VALUE;

        for (SpeedBuff buff : sortedBuffs) {
            if (!buff.isExpired()) {
                totalLevels += buff.getLevels();
                earliestExpiration = Math.min(
                        earliestExpiration,
                        buff.getExpireTimeMillis()
                );
            }
        }

        if (totalLevels <= 0) {
            applyBaseSpeed(player);
            return;
        }


        int resultAmplifier = totalLevels - 1;

        long remainingMs = Math.max(1L, earliestExpiration - now);
        int durationTicks = (int) Math.max(
                1L,
                (remainingMs + 49L) / 50L
        );

        PotionEffect existing = player.getPotionEffect(PotionEffectType.SPEED);

        if (existing != null
                && existing.getAmplifier() == resultAmplifier
                && existing.getDuration() >= durationTicks - 1) {
            return;
        }

        player.removePotionEffect(PotionEffectType.SPEED);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                durationTicks,
                resultAmplifier,
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
