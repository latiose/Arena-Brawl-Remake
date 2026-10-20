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

    public record ActiveBuff(double multiplier, long startedAt, long durationMillis, String title,
                             boolean isPowerup, boolean singleUse, Runnable onConsumed) {
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
                .add(new ActiveBuff(multiplier, System.currentTimeMillis(), durationMillis, title,
                        isPowerup, false, null));
    }


    public void applySingleUseBuff(Player player, double multiplier, long durationMillis,
                                   String title, Runnable onConsumed) {
        activeBuffs.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new ActiveBuff(multiplier, System.currentTimeMillis(), durationMillis, title,
                        false, true, onConsumed));
    }

    public double getMultiplier(Player player) {
        return getMultiplier(player, false);
    }


    public double getAndConsumeMultiplier(Player player) {
        double multiplier = getMultiplier(player, true);
        consumeSingleUseBuffs(player);
        return multiplier;
    }

    private double getMultiplier(Player player, boolean includeSingleUse) {
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
                if (!includeSingleUse && buff.singleUse()) {
                    continue;
                }
                if (highestRegularBuff == null || buff.multiplier() > highestRegularBuff.multiplier()) {
                    highestRegularBuff = buff;
                }
            }
        }

        double baseMultiplier = (highestRegularBuff != null) ? highestRegularBuff.multiplier() : 1.0;
        double extraPowerupMultiplier = (powerupBuff != null) ? powerupBuff.multiplier() - 1 : 0.0;
        double finalMultiplier = baseMultiplier + extraPowerupMultiplier;

        ActiveBuff buffToDisplay = (powerupBuff != null) ? powerupBuff : highestRegularBuff;

        if (buffToDisplay != null && !buffToDisplay.singleUse()) {
            double progress = (double) buffToDisplay.getRemainingMillis() / buffToDisplay.durationMillis();
            StatusBarUtil.sendStatusBar(player, buffToDisplay.title(), progress, "#FFA500");
        }

        return finalMultiplier;
    }

    private void consumeSingleUseBuffs(Player player) {
        List<ActiveBuff> buffs = activeBuffs.get(player.getUniqueId());
        if (buffs == null || buffs.isEmpty()) return;

        List<ActiveBuff> consumed = new ArrayList<>();
        for (ActiveBuff buff : buffs) {
            if (buff.singleUse() && !buff.isExpired()) {
                consumed.add(buff);
            }
        }
        if (consumed.isEmpty()) return;

        buffs.removeAll(consumed);
        if (buffs.isEmpty()) {
            activeBuffs.remove(player.getUniqueId());
        }

        for (ActiveBuff buff : consumed) {
            Runnable callback = buff.onConsumed();
            if (callback != null) {
                callback.run();
            }
        }
    }

    public void removeBuff(Player player, String title) {
        List<ActiveBuff> buffs = activeBuffs.get(player.getUniqueId());
        if (buffs == null) return;

        buffs.removeIf(buff -> buff.title().equalsIgnoreCase(title));

        if (buffs.isEmpty()) {
            activeBuffs.remove(player.getUniqueId());
        }
    }

    public void clear(Player player) {
        activeBuffs.remove(player.getUniqueId());
    }
}