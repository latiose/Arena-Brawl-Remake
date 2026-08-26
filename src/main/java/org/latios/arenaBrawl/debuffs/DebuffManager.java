package org.latios.arenaBrawl.debuffs;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
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

    private record ActiveDebuff(DebuffType type, UUID attackerId, long startedAt, long durationMillis) {
    }

    private final Map<UUID, ActiveDebuff> activeDebuffs = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Double> accumulatedDamage = new HashMap<>();
    private final List<DebuffListener> listeners = new ArrayList<>();

    public void registerListener(DebuffListener listener) {
        listeners.add(listener);
    }

    public boolean tryApply(Player attacker, Player victim, DebuffType type, long durationMillis) {
        if (hasActiveDebuff(victim)) {
            return false;
        }

        if (activeDebuffs.containsKey(victim.getUniqueId())) {
            clear(victim);
        }

        UUID attackerId = attacker != null ? attacker.getUniqueId() : null;
        activeDebuffs.put(victim.getUniqueId(), new ActiveDebuff(type, attackerId, System.currentTimeMillis(), durationMillis));
        accumulatedDamage.put(victim.getUniqueId(), 0.0);

        BossBar bar = Bukkit.createBossBar(type.getDisplayName(), BarColor.PURPLE, BarStyle.SOLID);
        bar.addPlayer(victim);
        bar.setProgress(1.0);
        bossBars.put(victim.getUniqueId(), bar);

        for (DebuffListener listener : listeners) {
            if (listener instanceof PoisonListener poisonListener) {
                poisonListener.onAppliedWithAttacker(victim, attacker, type);
            } else {
                listener.onApplied(victim, type);
            }
        }

        return true;
    }

    public boolean tryApply(Player victim, DebuffType type, long durationMillis) {
        return tryApply(null, victim, type, durationMillis);
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

    public boolean addAccumulatedDamage(Player player, double amount, double breakThreshold) {
        if (!hasActiveDebuff(player)) return false;

        double total = accumulatedDamage.getOrDefault(player.getUniqueId(), 0.0) + amount;
        accumulatedDamage.put(player.getUniqueId(), total);

        return total >= breakThreshold;
    }

    public void tick(Player player) {
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        if (debuff == null) return;
        if(debuff.type.equals(DebuffType.SLOW)) player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 10);
        if(debuff.type.equals(DebuffType.IMMOBILIZE)) player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 10);
        if(debuff.type.equals(DebuffType.STUN)) player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation(), 10);


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