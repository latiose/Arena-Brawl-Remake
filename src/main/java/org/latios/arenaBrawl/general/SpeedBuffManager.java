package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpeedBuffManager {

    public record ActiveBuff(int amplifier, long startedAt, long durationMillis) {
        public boolean isExpired() {
            return System.currentTimeMillis() - startedAt >= durationMillis;
        }

        public long getRemainingMillis() {
            return Math.max(0, durationMillis - (System.currentTimeMillis() - startedAt));
        }
    }

    private final Map<UUID, List<ActiveBuff>> activeBuffs = new HashMap<>();

    public void applyBuff(Player player, int amplifier, long durationMillis) {
        activeBuffs.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new ActiveBuff(amplifier, System.currentTimeMillis(), durationMillis));

        updatePlayerSpeed(player);
    }

    public void updatePlayerSpeed(Player player) {
        List<ActiveBuff> buffs = activeBuffs.get(player.getUniqueId());

        if (buffs != null) {
            buffs.removeIf(ActiveBuff::isExpired);
            if (buffs.isEmpty()) {
                activeBuffs.remove(player.getUniqueId());
                buffs = null;
            }
        }

        if (buffs == null || buffs.isEmpty()) {
            PotionEffect currentSpeed = player.getPotionEffect(PotionEffectType.SPEED);
            if (currentSpeed == null || currentSpeed.getAmplifier() != 0) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED,
                        PotionEffect.INFINITE_DURATION,
                        0, // Speed I Base
                        true,
                        false
                ));
            }
            return;
        }
        ActiveBuff highestBuff = null;
        for (ActiveBuff buff : buffs) {
            if (highestBuff == null || buff.amplifier() > highestBuff.amplifier()) {
                highestBuff = buff;
            }
        }

        if (highestBuff != null) {
            int durationTicks = (int) (highestBuff.getRemainingMillis() / 50L);
            PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);

            if (current == null || current.getAmplifier() != highestBuff.amplifier() || current.getDuration() < durationTicks - 5) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED,
                        durationTicks,
                        highestBuff.amplifier(),
                        true,
                        false
                ), true);
            }
        }
    }

    public void clear(Player player) {
        activeBuffs.remove(player.getUniqueId());
        updatePlayerSpeed(player);
    }
}