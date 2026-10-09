package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Bloodthirst implements Ability {

    private record Active(long endMs, double fraction) {}

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    private final long cooldownMs;
    private final long durationMs;
    private final double lifestealPercent;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Bloodthirst(CooldownManager cooldownManager, PlayerHealthManager healthManager,
                       CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.durationMs = config.getLong("duration-ms", 8000L);
        this.lifestealPercent = config.getDouble("lifesteal-percent", 50.0);
        this.cost = new CooldownCost(cooldownManager, "bloodthirst", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Bloodthirst";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        UUID id = player.getUniqueId();
        Active active = new Active(System.currentTimeMillis() + durationMs, lifestealPercent / 100.0);
        ACTIVE.put(id, active);

        MatchSoundUtils.play(config, player, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.6f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                30, 0.5, 0.8, 0.5, 0, new Particle.DustOptions(Color.RED, 1.4f));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (ACTIVE.get(id) != active) {
                    cancel();
                    return;
                }
                if (!player.isOnline() || player.isDead()) {
                    ACTIVE.remove(id);
                    cancel();
                    return;
                }
                if (System.currentTimeMillis() >= active.endMs()) {
                    ACTIVE.remove(id);
                    player.sendMessage(MessageUtils.negative() + "§3Your Bloodthirst has ended.");
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                        4, 0.4, 0.7, 0.4, 0, new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f));
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 5L);

        return true;
    }

    public static void onDamageDealt(Player attacker, Player victim, double damage,
                                     PlayerHealthManager healthManager) {
        if (attacker == null || attacker.equals(victim)) return;

        Active active = ACTIVE.get(attacker.getUniqueId());
        if (active == null) return;

        if (System.currentTimeMillis() >= active.endMs()) {
            ACTIVE.remove(attacker.getUniqueId());
            return;
        }

        double heal = damage * active.fraction();
        if (heal <= 0) return;

        healthManager.heal(attacker, heal, "Bloodthirst");
        attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1.8, 0), 2, 0.3, 0.2, 0.3, 0);
    }

    @Override
    public String getDescription() {
        return "For " + (durationMs / 1000L) + "s, heals you for " + (int) lifestealPercent
                + "% of all the damage you deal.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Lifesteal", (int) lifestealPercent + "%"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}