
package org.latios.arenaBrawl.debuffs;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DebuffManager {

    private record ActiveDebuff(DebuffType type, long startedAt, long durationMillis) {
    }

    private final Map<UUID, ActiveDebuff> activeDebuffs = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Double> accumulatedDamage = new HashMap<>();
    private final List<DebuffListener> listeners = new ArrayList<>();

    public void registerListener(DebuffListener listener) {
        listeners.add(listener);
    }

    public boolean tryApply(Player player, DebuffType type, long durationMillis) {
        if (hasActiveDebuff(player)) {
            return false;
        }

        // The player might still have a stale entry that expired by time but was never
        // formally cleared yet (clear() only runs via the periodic tick task). Force a
        // clean-up here to avoid orphaned boss bars / lingering potion effects before
        // overwriting the map entries with a new debuff.
        if (activeDebuffs.containsKey(player.getUniqueId())) {
            clear(player);
        }

        activeDebuffs.put(player.getUniqueId(), new ActiveDebuff(type, System.currentTimeMillis(), durationMillis));
        accumulatedDamage.put(player.getUniqueId(), 0.0);

        BossBar bar = Bukkit.createBossBar(type.getDisplayName(), BarColor.RED, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setProgress(1.0);
        bossBars.put(player.getUniqueId(), bar);

        for (DebuffListener listener : listeners) {
            listener.onApplied(player, type);
        }

        return true;
    }

    public boolean hasActiveDebuff(Player player) {
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        if (debuff == null) return false;
        return System.currentTimeMillis() - debuff.startedAt() < debuff.durationMillis();
    }

    public boolean hasDebuff(Player player, DebuffType type) {
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        return debuff != null && debuff.type() == type && hasActiveDebuff(player);
    }

    /**
     * Adds damage to the accumulated total for the player's active debuff.
     * Returns true if the accumulated damage reached the given threshold (caller should then clear the debuff).
     */
    public boolean addAccumulatedDamage(Player player, double amount, double breakThreshold) {
        if (!hasActiveDebuff(player)) return false;

        double total = accumulatedDamage.getOrDefault(player.getUniqueId(), 0.0) + amount;
        accumulatedDamage.put(player.getUniqueId(), total);

        return total >= breakThreshold;
    }

    public void tick(Player player) {
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        if (debuff == null) return;

        long elapsed = System.currentTimeMillis() - debuff.startedAt();
        long remaining = debuff.durationMillis() - elapsed;

        if (remaining <= 0) {
            clear(player);
            return;
        }

        double progress = (double) remaining / debuff.durationMillis();
        BossBar bar = bossBars.get(player.getUniqueId());
        if (bar != null) {
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }
    }

    public void clear(Player player) {
        ActiveDebuff debuff = activeDebuffs.remove(player.getUniqueId());
        accumulatedDamage.remove(player.getUniqueId());
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }

        if (debuff != null) {
            for (DebuffListener listener : listeners) {
                listener.onExpired(player, debuff.type());
            }
            List<PotionEffect> effectsSnapshot = new ArrayList<>(player.getActivePotionEffects());
            for (PotionEffect effect : effectsSnapshot) {
                if (effect.getType() != PotionEffectType.SPEED && effect.getType() != PotionEffectType.INVISIBILITY) {
                    player.removePotionEffect(effect.getType());
                }
            }
        }
    }
}