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

    /**
     * A single-use buff is meant to empower one attack, but one attack can hit
     * several targets (AoE abilities, whirlwind-style swings). Each target is
     * resolved with its own damage call, so after the first one consumes the
     * buff the rest of the same attack must still see its multiplier. Consumed
     * bonuses stay valid for this short window, which covers every target of
     * the same hit while being far shorter than any ability cooldown.
     */
    private static final long CONSUMED_WINDOW_MILLIS = 100L;

    private record ConsumedBonus(double multiplier, long consumedAt) {
        public boolean isExpired() {
            return System.currentTimeMillis() - consumedAt > CONSUMED_WINDOW_MILLIS;
        }
    }

    private final Map<UUID, List<ActiveBuff>> activeBuffs = new HashMap<>();
    private final Map<UUID, List<ConsumedBonus>> consumedBonuses = new HashMap<>();

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
        double consumedBonus = includeSingleUse ? getConsumedBonusMultiplier(player) : 1.0;

        List<ActiveBuff> buffs = activeBuffs.get(player.getUniqueId());
        if (buffs == null || buffs.isEmpty()) return consumedBonus;

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
            return consumedBonus;
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
        baseMultiplier = Math.max(baseMultiplier, consumedBonus);
        double extraPowerupMultiplier = (powerupBuff != null) ? powerupBuff.multiplier() - 1 : 0.0;
        double finalMultiplier = baseMultiplier + extraPowerupMultiplier;

        ActiveBuff buffToDisplay = (powerupBuff != null) ? powerupBuff : highestRegularBuff;

        if (buffToDisplay != null && !buffToDisplay.singleUse()) {
            double progress = (double) buffToDisplay.getRemainingMillis() / buffToDisplay.durationMillis();
            StatusBarUtil.sendStatusBar(player, buffToDisplay.title(), progress, "#FFA500");
        }

        return finalMultiplier;
    }

    /**
     * Highest multiplier from single-use buffs consumed within the current
     * attack window, or {@code 1.0} when there is none.
     */
    private double getConsumedBonusMultiplier(Player player) {
        List<ConsumedBonus> bonuses = consumedBonuses.get(player.getUniqueId());
        if (bonuses == null) return 1.0;

        bonuses.removeIf(ConsumedBonus::isExpired);
        if (bonuses.isEmpty()) {
            consumedBonuses.remove(player.getUniqueId());
            return 1.0;
        }

        double highest = 1.0;
        for (ConsumedBonus bonus : bonuses) {
            highest = Math.max(highest, bonus.multiplier());
        }
        return highest;
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

        // Register the bonus before running callbacks: the callback may remove
        // the buff's title, and every target of the same attack still needs it.
        long now = System.currentTimeMillis();
        List<ConsumedBonus> bonuses = consumedBonuses.computeIfAbsent(
                player.getUniqueId(), k -> new ArrayList<>());
        for (ActiveBuff buff : consumed) {
            bonuses.add(new ConsumedBonus(buff.multiplier(), now));
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
        consumedBonuses.remove(player.getUniqueId());
    }
}