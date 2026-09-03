package org.latios.arenaBrawl.powerups;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.StatusBarUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DamageBuffManager {

    public record ActiveBuff(double multiplier, long startedAt, long durationMillis, String title, boolean isPowerup) {
        public boolean isExpired() {
            return System.currentTimeMillis() - startedAt >= durationMillis;
        }

        public long getRemainingMillis() {
            return Math.max(0, durationMillis - (System.currentTimeMillis() - startedAt));
        }
    }

    private final Map<UUID, List<ActiveBuff>> activeBuffs = new HashMap<>();

    public void applyBuff(Player player, double multiplier, long durationMillis, String title) {
        applyBuff(player, multiplier, durationMillis, title, false);
    }

    public void applyBuff(Player player, double multiplier, long durationMillis, String title, boolean isPowerup) {
        activeBuffs.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new ActiveBuff(multiplier, System.currentTimeMillis(), durationMillis, title, isPowerup));
    }

    public double getMultiplier(Player player) {
        List<ActiveBuff> buffs = activeBuffs.get(player.getUniqueId());
        if (buffs == null || buffs.isEmpty()) return 1.0;

        List<ActiveBuff> expiredBuffs = new ArrayList<>();
        for (ActiveBuff buff : buffs) {
            if (buff.isExpired()) {
                expiredBuffs.add(buff);
            }
        }

        if (!expiredBuffs.isEmpty()) {
            buffs.removeAll(expiredBuffs);
            for (ActiveBuff expired : expiredBuffs) {
                Component expireMessage = Component.text("Your ", NamedTextColor.YELLOW)
                        .append(Component.text(expired.title().toUpperCase(), NamedTextColor.RED, TextDecoration.BOLD))
                        .append(Component.text(" has expired!", NamedTextColor.YELLOW));
                player.sendMessage(expireMessage);
            }
        }

        if (buffs.isEmpty()) {
            activeBuffs.remove(player.getUniqueId());
            return 1.0;
        }

        ActiveBuff highestRegularBuff = null;
        ActiveBuff powerupBuff = null;

        for (ActiveBuff buff : buffs) {
            if (buff.isPowerup()) {
                if (powerupBuff == null || buff.multiplier() > powerupBuff.multiplier()) {
                    powerupBuff = buff;
                }
            } else {
                if (highestRegularBuff == null || buff.multiplier() > highestRegularBuff.multiplier()) {
                    highestRegularBuff = buff;
                }
            }
        }

        double baseMultiplier = (highestRegularBuff != null) ? highestRegularBuff.multiplier() : 1.0;
        double extraPowerupMultiplier = (powerupBuff != null) ? powerupBuff.multiplier() - 1 : 0.0;
        double finalMultiplier = baseMultiplier + extraPowerupMultiplier;

        ActiveBuff buffToDisplay = (powerupBuff != null) ? powerupBuff : highestRegularBuff;
        if (buffToDisplay != null) {
            double progress = (double) buffToDisplay.getRemainingMillis() / buffToDisplay.durationMillis();
            StatusBarUtil.sendStatusBar(player, buffToDisplay.title(), progress, "#FFA500");
        }

        return finalMultiplier;
    }

    public void clear(Player player) {
        activeBuffs.remove(player.getUniqueId());
    }
}