// debuffs/DebuffManager.java
package org.latios.arenaBrawl.debuffs;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DebuffManager {

    private record ActiveDebuff(DebuffType type, long startedAt, long durationMillis) {}

    private final Map<UUID, ActiveDebuff> activeDebuffs = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    /**
     * Attempts to apply a debuff. Does nothing if the player already has one active.
     * Returns true if the debuff was applied, false if it was blocked.
     */
    public boolean tryApply(Player player, DebuffType type, long durationMillis) {
        if (hasActiveDebuff(player)) {
            return false;
        }

        activeDebuffs.put(player.getUniqueId(), new ActiveDebuff(type, System.currentTimeMillis(), durationMillis));

        BossBar bar = Bukkit.createBossBar(type.getDisplayName(), BarColor.RED, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setProgress(1.0);
        bossBars.put(player.getUniqueId(), bar);

        return true;
    }

    public boolean hasActiveDebuff(Player player) {
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        if (debuff == null) return false;

        long elapsed = System.currentTimeMillis() - debuff.startedAt();
        return elapsed < debuff.durationMillis();
    }

    public boolean hasDebuff(Player player, DebuffType type) {
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        return debuff != null && debuff.type() == type && hasActiveDebuff(player);
    }

    /**
     * Called periodically to update the boss bar progress and clear expired debuffs.
     */
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
        activeDebuffs.remove(player.getUniqueId());
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.SPEED || effect.getType() != PotionEffectType.INVISIBILITY) {
                player.removePotionEffect(effect.getType());
            }
        }
    }
}