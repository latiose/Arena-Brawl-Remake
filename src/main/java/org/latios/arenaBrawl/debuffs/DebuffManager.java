package org.latios.arenaBrawl.debuffs;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.abilities.support.SongOfPowerManager;
import org.latios.arenaBrawl.general.StatusBarUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DebuffManager {

    private record ActiveDebuff(
            DebuffType type,
            UUID attackerId,
            long startedAt,
            long durationMillis,
            double strength
    ) {}

    private final Map<UUID, ActiveDebuff> activeDebuffs = new HashMap<>();
    private final Map<UUID, List<ActiveDebuff>> activeAntiHeals = new HashMap<>();
    private final Map<UUID, Double> accumulatedDamage = new HashMap<>();
    private final List<DebuffListener> listeners = new ArrayList<>();
    private SongOfPowerManager songOfPowerManager;

    public void registerListener(DebuffListener listener) {
        listeners.add(listener);
    }

    public void setSongOfPowerManager(SongOfPowerManager songOfPowerManager) {
        this.songOfPowerManager = songOfPowerManager;
    }

    public boolean tryApply(Player attacker, Player victim, DebuffType type, long durationMillis) {
        return tryApply(attacker, victim, type, durationMillis, 0.0);
    }

    public boolean tryApply(
            Player attacker,
            Player victim,
            DebuffType type,
            long durationMillis,
            double strength
    ) {
        if (songOfPowerManager != null && songOfPowerManager.isActive(victim)) {
            return false;
        }
        if(victim.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        if (type != DebuffType.ANTIHEAL && hasPrimaryDebuff(victim)) {
            return false;
        }

        UUID victimId = victim.getUniqueId();
        ActiveDebuff debuff = new ActiveDebuff(
                type,
                attacker != null ? attacker.getUniqueId() : null,
                System.currentTimeMillis(),
                durationMillis,
                strength
        );

        if (type == DebuffType.ANTIHEAL) {
            activeAntiHeals.computeIfAbsent(victimId, ignored -> new ArrayList<>()).add(debuff);
        } else {
            if (activeDebuffs.containsKey(victimId)) {
                clearPrimary(victim);
            }
            activeDebuffs.put(victimId, debuff);
            accumulatedDamage.put(victimId, 0.0);
        }

        for (DebuffListener listener : listeners) {
            if (listener instanceof PoisonListener poisonListener) {
                poisonListener.onAppliedWithAttacker(victim, attacker, type, durationMillis);
            } else {
                listener.onApplied(victim, type, durationMillis, strength);
            }
        }

        return true;
    }

    public boolean tryApply(Player victim, DebuffType type, long durationMillis) {
        return tryApply(null, victim, type, durationMillis);
    }

    public boolean hasActiveDebuff(Player player) {
        if (hasPrimaryDebuff(player)) {
            return true;
        }

        return hasDebuff(player, DebuffType.ANTIHEAL);
    }

    private boolean hasPrimaryDebuff(Player player) {
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        return debuff != null
                && System.currentTimeMillis() - debuff.startedAt() < debuff.durationMillis();
    }

    public boolean hasDebuff(Player player, DebuffType type) {
        if (type == DebuffType.ANTIHEAL) {
            return activeAntiHeals.getOrDefault(player.getUniqueId(), List.of()).stream()
                    .anyMatch(active -> System.currentTimeMillis() - active.startedAt() < active.durationMillis());
        }
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        return debuff != null
                && debuff.type() == type
                && hasPrimaryDebuff(player);
    }

    public boolean addAccumulatedDamage(Player player, double amount, double breakThreshold) {
        if (!hasActiveDebuff(player)) return false;

        double total = accumulatedDamage.getOrDefault(player.getUniqueId(), 0.0) + amount;
        accumulatedDamage.put(player.getUniqueId(), total);

        return total >= breakThreshold;
    }

    public void tick(Player player) {
        tickAntiHeals(player);
        ActiveDebuff debuff = activeDebuffs.get(player.getUniqueId());
        if (debuff == null) return;

        long elapsed = System.currentTimeMillis() - debuff.startedAt();
        long remaining = debuff.durationMillis() - elapsed;

        if (remaining <= 0) {
            clearPrimary(player);
            return;
        }

        Location bodyLoc = player.getLocation().add(0, 1.0, 0);

        if (debuff.type.equals(DebuffType.SLOW)) {
            player.getWorld().spawnParticle(Particle.WITCH, bodyLoc, 2, 0.3, 0.5, 0.3, 0.05);
        } else if (debuff.type.equals(DebuffType.IMMOBILIZE)) {
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, bodyLoc, 2, 0.3, 0.5, 0.3, 0.05);
        } else if (debuff.type.equals(DebuffType.STUN) || debuff.type.equals(DebuffType.POLYMORPH)) {
            player.getWorld().spawnParticle(Particle.WHITE_ASH, bodyLoc, 10, 0.2, 0.2, 0.2, 0.01);
        } else if (debuff.type.equals(DebuffType.POISON)) {
            player.getWorld().spawnParticle(Particle.ITEM_SLIME, bodyLoc, 2, 0.2, 0.2, 0.2, 0.01);
        } else if (debuff.type.equals(DebuffType.ANTIHEAL)) {
            player.getWorld().spawnParticle(Particle.SQUID_INK, bodyLoc, 2, 0.3, 0.5, 0.3, 0.02);
            player.getWorld().spawnParticle(Particle.SMOKE, bodyLoc, 2, 0.2, 0.4, 0.2, 0.01);
        } else if (debuff.type.equals(DebuffType.SILENCE)) {
            Location headLoc = player.getLocation().add(0, 2.0, 0);
            player.getWorld().spawnParticle(Particle.ENCHANT, headLoc, 5, 0.3, 0.3, 0.3, 0.5);
            player.getWorld().spawnParticle(Particle.WAX_OFF, headLoc, 1, 0.2, 0.2, 0.2, 0.0);
        }

        double progress = (double) remaining / debuff.durationMillis();
        StatusBarUtil.sendStatusBar(player, debuff.type().getDisplayName(), progress, debuff.type.getColor()); //color
    }

    private void tickAntiHeals(Player player) {
        UUID playerId = player.getUniqueId();
        List<ActiveDebuff> debuffs = activeAntiHeals.get(playerId);
        if (debuffs == null) return;

        long now = System.currentTimeMillis();
        debuffs.removeIf(debuff -> {
            if (now - debuff.startedAt() < debuff.durationMillis()) return false;
            notifyExpired(player, debuff);
            return true;
        });

        if (debuffs.isEmpty()) {
            activeAntiHeals.remove(playerId);
        }
    }

    private void clearPrimary(Player player) {
        ActiveDebuff debuff = activeDebuffs.remove(player.getUniqueId());
        accumulatedDamage.remove(player.getUniqueId());
        if (debuff == null) return;

        notifyExpired(player, debuff);
        List<PotionEffect> effectsSnapshot = new ArrayList<>(player.getActivePotionEffects());
        for (PotionEffect effect : effectsSnapshot) {
            if (effect.getType() != PotionEffectType.SPEED
                    && effect.getType() != PotionEffectType.INVISIBILITY) {
                player.removePotionEffect(effect.getType());
            }
        }
    }

    public void clear(Player player) {
        List<ActiveDebuff> antiHeals = activeAntiHeals.remove(player.getUniqueId());
        if (antiHeals != null) {
            for (ActiveDebuff debuff : antiHeals) {
                notifyExpired(player, debuff);
            }
        }

        ActiveDebuff debuff = activeDebuffs.remove(player.getUniqueId());
        accumulatedDamage.remove(player.getUniqueId());

        if (debuff != null) {
            notifyExpired(player, debuff);
            List<PotionEffect> effectsSnapshot = new ArrayList<>(player.getActivePotionEffects());
            for (PotionEffect effect : effectsSnapshot) {
                if (effect.getType() != PotionEffectType.SPEED && effect.getType() != PotionEffectType.INVISIBILITY) {
                    player.removePotionEffect(effect.getType());
                }
            }
        }
    }

    private void notifyExpired(Player player, ActiveDebuff debuff) {
        for (DebuffListener listener : listeners) {
            listener.onExpired(player, debuff.type(), debuff.durationMillis(), debuff.strength());
        }
    }

    public Player getAttacker(Player victim) {
        ActiveDebuff debuff = activeDebuffs.get(victim.getUniqueId());
        if (debuff == null || debuff.attackerId() == null) return null;
        return Bukkit.getPlayer(debuff.attackerId());
    }
}