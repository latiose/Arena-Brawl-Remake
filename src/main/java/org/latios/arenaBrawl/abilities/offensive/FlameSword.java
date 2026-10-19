package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MeleeHitEffect;
import org.latios.arenaBrawl.general.MessageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FlameSword implements Ability, MeleeHitEffect {
    private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitRunnable> TASKS = new ConcurrentHashMap<>();
    private final double damageMultiplier;
    private final double energyCost;
    private final long durationTicks;
    private final long particleIntervalTicks;
    private final AbilityConfig config;
    private final AbilityCost cost;
    private final Map<UUID, Long> activeUntil = new HashMap<>();
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();
    private final EnergyModifierManager energyManager;
    private final double energyModifier;
    public FlameSword(EnergyManager energyManager, EnergyModifierManager energyModifierManager, AbilityConfig config) {
        this.damageMultiplier = config.getDouble("damage-multiplier", 4.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.durationTicks = config.getLong("duration-ticks", 120L);
        this.particleIntervalTicks = config.getLong("particle-interval-ticks", 20L);
        this.energyModifier = config.getDouble("energy-modifier", 0.8);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.config = config;
        this.energyManager = energyModifierManager;
    }

    @Override
    public String getName() { return "Flame Sword"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Engulfs your weapon in flames for " + (durationTicks * 0.05) + " seconds. "
                + "Your melee attacks deal " + trim(damageMultiplier) + "x damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage Multiplier", trim(damageMultiplier) + "x"),
                new AbilityStat("Energy Modifier", trim(energyModifier) + "x"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Duration", (durationTicks * 0.05) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        UUID id = player.getUniqueId();
        stop(id);
        ACTIVE_UNTIL.put(id, System.currentTimeMillis() + durationTicks * 50L);
        energyManager.addModifier(player, "flame_sword", energyModifier, durationTicks * 50L);
        MatchSoundUtils.play(config, player, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.7f);
        MatchSoundUtils.play(config, player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.08);

        BukkitRunnable task = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (!player.isOnline() || player.isDead() || ticks > durationTicks) {
                    if (player.isOnline() && !player.isDead()) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.2f);
                    }
                    stop(id);
                    return;
                }

                if (ticks % particleIntervalTicks == 0) {
                    player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0),
                            8, 0.6, 0.7, 0.6, 0.0);
                }
            }
        };
        TASKS.put(id, task);
        task.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        return true;
    }

    @Override
    public void onMatchStart(Player player) {
        Ability.super.onMatchStart(player);
        stop(player.getUniqueId());
    }

    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        if (!isActive(attacker)) return;

        Location loc = impactLocation != null ? impactLocation : victim.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 25, 0.3, 0.4, 0.3, 0.08);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 5, 0.3, 0.3, 0.3, 0.0);

        int roundedDamage = (int) Math.round(finalDamage);
        attacker.sendMessage(MessageUtils.positive() + String.format(
                "§3Your %s hit §3%s §3for §c%d §3damage.",
                getName(), victim.getName(), roundedDamage
        ));

        victim.sendMessage(MessageUtils.negative() + String.format(
                "§3%s's %s hit §3you §3for §c%d §3damage.",
                attacker.getName(), getName(), roundedDamage
        ));
    }

    @Override
    public double getMultiplier(Player attacker) {
        return isActive(attacker) ? damageMultiplier : 1.0;
    }

    private boolean isActive(Player player) {
        Long until = ACTIVE_UNTIL.get(player.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            stop(player.getUniqueId());
            return false;
        }
        return true;
    }

    private void stop(UUID id) {
        ACTIVE_UNTIL.remove(id);
        BukkitRunnable task = TASKS.remove(id);
        if (task != null) {
            try { task.cancel(); } catch (IllegalStateException ignored) {}
        }
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }
}