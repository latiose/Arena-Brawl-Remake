package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.general.HologramUtils;
import org.latios.arenaBrawl.general.MeleeHitEffect;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.stats.StatsManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stores the unlocked/equipped cosmetic effects for every player and plays
 * them at the right moment. All effects are cosmetic only.
 */
public class CosmeticEffectManager implements MeleeHitEffect {

    public enum PurchaseResult {
        SUCCESS,
        NOT_ENOUGH_COINS,
        ALREADY_UNLOCKED
    }

    private final Plugin plugin;
    private final StatsManager statsManager;
    private final PlayerHealthManager healthManager;
    private final File file;
    private final YamlConfiguration config;

    private final Map<UUID, Set<HitEffectType>> unlockedHitEffects = new HashMap<>();
    private final Map<UUID, HitEffectType> equippedHitEffects = new HashMap<>();
    private final Map<UUID, Set<KillEffectType>> unlockedKillEffects = new HashMap<>();
    private final Map<UUID, KillEffectType> equippedKillEffects = new HashMap<>();

    public CosmeticEffectManager(Plugin plugin, StatsManager statsManager, PlayerHealthManager healthManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.healthManager = healthManager;
        this.file = new File(plugin.getDataFolder(), "cosmetics.yml");

        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create cosmetics.yml", e);
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadForPlayer(Player player) {
        UUID id = player.getUniqueId();

        Set<HitEffectType> hitEffects = new HashSet<>();
        for (String value : config.getStringList(id + ".hit-effects.unlocked")) {
            HitEffectType type = parseHitEffect(value);
            if (type != null) {
                hitEffects.add(type);
            }
        }
        unlockedHitEffects.put(id, hitEffects);

        HitEffectType equippedHit = parseHitEffect(config.getString(id + ".hit-effects.equipped"));
        equippedHitEffects.put(id, equippedHit != null && hitEffects.contains(equippedHit) ? equippedHit : null);

        Set<KillEffectType> killEffects = new HashSet<>();
        for (String value : config.getStringList(id + ".kill-effects.unlocked")) {
            KillEffectType type = parseKillEffect(value);
            if (type != null) {
                killEffects.add(type);
            }
        }
        unlockedKillEffects.put(id, killEffects);

        KillEffectType equippedKill = parseKillEffect(config.getString(id + ".kill-effects.equipped"));
        equippedKillEffects.put(id, equippedKill != null && killEffects.contains(equippedKill) ? equippedKill : null);
    }

    public void unloadPlayer(Player player) {
        UUID id = player.getUniqueId();
        unlockedHitEffects.remove(id);
        equippedHitEffects.remove(id);
        unlockedKillEffects.remove(id);
        equippedKillEffects.remove(id);
    }

    /* --------------------------------------------------------------------- */
    /* Hit effects                                                           */
    /* --------------------------------------------------------------------- */

    public boolean isHitUnlocked(Player player, HitEffectType type) {
        return unlockedHitEffects.getOrDefault(player.getUniqueId(), Set.of()).contains(type);
    }

    public HitEffectType getEquippedHitEffect(Player player) {
        return equippedHitEffects.get(player.getUniqueId());
    }

    public PurchaseResult buyHitEffect(Player player, HitEffectType type) {
        if (isHitUnlocked(player, type)) {
            return PurchaseResult.ALREADY_UNLOCKED;
        }

        var stats = statsManager.getStats(player);
        if (stats.coins < type.getPrice()) {
            return PurchaseResult.NOT_ENOUGH_COINS;
        }

        stats.coins -= type.getPrice();
        statsManager.saveDirectly(player, stats);

        unlockedHitEffects.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(type);
        equippedHitEffects.put(player.getUniqueId(), type);
        save(player);

        return PurchaseResult.SUCCESS;
    }

    public void equipHitEffect(Player player, HitEffectType type) {
        if (!isHitUnlocked(player, type)) return;
        equippedHitEffects.put(player.getUniqueId(), type);
        save(player);
    }

    public void unequipHitEffect(Player player) {
        equippedHitEffects.remove(player.getUniqueId());
        save(player);
    }

    /* --------------------------------------------------------------------- */
    /* Kill effects                                                          */
    /* --------------------------------------------------------------------- */

    public boolean isKillUnlocked(Player player, KillEffectType type) {
        return unlockedKillEffects.getOrDefault(player.getUniqueId(), Set.of()).contains(type);
    }

    public KillEffectType getEquippedKillEffect(Player player) {
        return equippedKillEffects.get(player.getUniqueId());
    }

    public PurchaseResult buyKillEffect(Player player, KillEffectType type) {
        if (isKillUnlocked(player, type)) {
            return PurchaseResult.ALREADY_UNLOCKED;
        }

        var stats = statsManager.getStats(player);
        if (stats.coins < type.getPrice()) {
            return PurchaseResult.NOT_ENOUGH_COINS;
        }

        stats.coins -= type.getPrice();
        statsManager.saveDirectly(player, stats);

        unlockedKillEffects.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(type);
        equippedKillEffects.put(player.getUniqueId(), type);
        save(player);

        return PurchaseResult.SUCCESS;
    }

    public void equipKillEffect(Player player, KillEffectType type) {
        if (!isKillUnlocked(player, type)) return;
        equippedKillEffects.put(player.getUniqueId(), type);
        save(player);
    }

    public void unequipKillEffect(Player player) {
        equippedKillEffects.remove(player.getUniqueId());
        save(player);
    }

    /* --------------------------------------------------------------------- */
    /* Combat hooks                                                          */
    /* --------------------------------------------------------------------- */

    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        if (attacker == null || victim == null || finalDamage <= 0) return;

        HitEffectType effect = equippedHitEffects.get(attacker.getUniqueId());
        if (effect == null) return;

        Location location = impactLocation != null
                ? impactLocation.clone().add(
                        CosmeticEffects.random(-0.6, 0.6),
                        CosmeticEffects.random(-0.3, 0.5),
                        CosmeticEffects.random(-0.6, 0.6))
                : HologramUtils.randomAround(victim);
        effect.play(attacker, victim, location);
    }

    @Override
    public double getMultiplier(Player attacker) {
        return 1.0;
    }

    /** Called from the elimination callback after the normal death handling. */
    public void onPlayerEliminated(Player victim) {
        UUID attackerId = healthManager.getLastAttacker(victim);
        if (attackerId == null || attackerId.equals(victim.getUniqueId())) return;

        Player killer = Bukkit.getPlayer(attackerId);
        if (killer == null || !killer.isOnline()) return;

        KillEffectType effect = equippedKillEffects.get(killer.getUniqueId());
        if (effect == null) return;

        effect.play(plugin, killer, victim.getLocation());
    }

    /* --------------------------------------------------------------------- */
    /* Persistence                                                           */
    /* --------------------------------------------------------------------- */

    private HitEffectType parseHitEffect(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return HitEffectType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private KillEffectType parseKillEffect(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return KillEffectType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void save(Player player) {
        UUID id = player.getUniqueId();

        List<String> hitUnlocked = unlockedHitEffects.getOrDefault(id, Set.of())
                .stream()
                .map(Enum::name)
                .sorted()
                .toList();
        config.set(id + ".hit-effects.unlocked", new ArrayList<>(hitUnlocked));

        HitEffectType equippedHit = equippedHitEffects.get(id);
        config.set(id + ".hit-effects.equipped", equippedHit != null ? equippedHit.name() : null);

        List<String> killUnlocked = unlockedKillEffects.getOrDefault(id, Set.of())
                .stream()
                .map(Enum::name)
                .sorted()
                .toList();
        config.set(id + ".kill-effects.unlocked", new ArrayList<>(killUnlocked));

        KillEffectType equippedKill = equippedKillEffects.get(id);
        config.set(id + ".kill-effects.equipped", equippedKill != null ? equippedKill.name() : null);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save cosmetics.yml", e);
        }
    }
}
