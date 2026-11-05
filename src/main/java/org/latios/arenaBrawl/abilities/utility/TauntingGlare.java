package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class TauntingGlare implements Ability {

    /** Mutable because the damage-to-caster accumulator breaks the glare early. */
    public static final class GlareState {
        final UUID casterId;
        final long endMs;
        final double casterDamageBonus;
        final double allyDamageReduction;
        final double endDamageThreshold;
        double damageDealtToCaster;

        GlareState(UUID casterId, long endMs, double casterDamageBonus,
                   double allyDamageReduction, double endDamageThreshold) {
            this.casterId = casterId;
            this.endMs = endMs;
            this.casterDamageBonus = casterDamageBonus;
            this.allyDamageReduction = allyDamageReduction;
            this.endDamageThreshold = endDamageThreshold;
        }
    }

    private static final Map<UUID, GlareState> ACTIVE_GLARES = new ConcurrentHashMap<>();

    public static GlareState getActive(Player tauntedPlayer) {
        GlareState state = ACTIVE_GLARES.get(tauntedPlayer.getUniqueId());
        if (state == null) return null;

        if (System.currentTimeMillis() >= state.endMs) {
            ACTIVE_GLARES.remove(tauntedPlayer.getUniqueId());
            return null;
        }
        return state;
    }

    public static void clear(Player player) {
        ACTIVE_GLARES.remove(player.getUniqueId());
    }


    public static double getDamageMultiplier(Player attacker, Player victim, TeamManager teamManager, double incomingDamage) {
        GlareState state = ACTIVE_GLARES.get(attacker.getUniqueId());
        if (state == null) return 1.0;

        if (System.currentTimeMillis() >= state.endMs) {
            ACTIVE_GLARES.remove(attacker.getUniqueId());
            return 1.0;
        }

        if (victim.getUniqueId().equals(state.casterId)) {
            state.damageDealtToCaster += incomingDamage;

            if (state.damageDealtToCaster >= state.endDamageThreshold) {
                ACTIVE_GLARES.remove(attacker.getUniqueId());

                Player caster = Bukkit.getPlayer(state.casterId);
                Player taunted = attacker;

                if (caster != null && caster.isOnline()) {
                    caster.sendMessage(MessageUtils.negative() + String.format(
                            "§3Your Taunting Glare was broken by §c%s§3!", taunted.getName()));
                }
                taunted.sendMessage(MessageUtils.positive() + String.format(
                        "§3You broke §c%s's §3Taunting Glare!"
                        ,caster.getName()));
            }

            return 1.0 + state.casterDamageBonus;
        }

        Player caster = Bukkit.getPlayer(state.casterId);
        if (caster != null && teamManager.isAlly(victim, caster)) {
            victim.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                    victim.getLocation().add(0, 2.1, 0), 1, 0.2, 0.1, 0.2, 0.0);
            return 1.0 - state.allyDamageReduction;
        }

        return 1.0;
    }

    private final Plugin plugin;
    private final long cooldownMs;
    private final long durationMs;
    private final double maxRange;
    private final double casterDamageBonus;
    private final double allyDamageReduction;
    private final double endDamageThreshold;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final AbilityConfig config;

    public TauntingGlare(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                         CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.durationMs = config.getLong("duration-ms", 8000L);
        this.maxRange = config.getDouble("max-range", 15.0);
        this.casterDamageBonus = config.getDouble("caster-damage-bonus", 0.25);
        this.allyDamageReduction = config.getDouble("ally-damage-reduction", 0.50);
        this.endDamageThreshold = config.getDouble("end-damage-threshold", 100.0);

        this.cost = new CooldownCost(cooldownManager, "tauntingglare", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Taunting Glare";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Taunt a target enemy: they deal " + Math.round(allyDamageReduction * 100) + "% less damage to your "
                + "allies, but " + Math.round(casterDamageBonus * 100) + "% more damage to you. Lasts "
                + (durationMs / 1000) + "s, or ends early when they deal " + (int) endDamageThreshold + " damage to you.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Ally Damage Reduction", Math.round(allyDamageReduction * 100) + "%"),
                new AbilityStat("Damage To You", "+" + Math.round(casterDamageBonus * 100) + "%"),
                new AbilityStat("Duration", (durationMs / 1000) + "s"),
                new AbilityStat("Break At", (int) endDamageThreshold + " damage to you"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        UUID targetId = target.getUniqueId();

        GlareState state = new GlareState(
                player.getUniqueId(),
                System.currentTimeMillis() + durationMs,
                casterDamageBonus,
                allyDamageReduction,
                endDamageThreshold
        );
        ACTIVE_GLARES.put(targetId, state);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 10.0f, 1.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 10.0f, 0.8f);
        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                target.getLocation().add(0, 2.2, 0), 10, 0.3, 0.3, 0.3, 0.0);


        target.sendMessage(MessageUtils.negative() + String.format(
                "§c%s §3taunted you with a glare!",
                player.getName()
        ));

        // Optional visual loop while the glare is active.
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                GlareState active = ACTIVE_GLARES.get(targetId);
                if (active == null || active != state
                        || System.currentTimeMillis() >= active.endMs
                        || !target.isOnline() || target.isDead()
                        || !player.isOnline()) {
                    cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                        target.getLocation().add(0, 2.2, 0), 1, 0.2, 0.1, 0.2, 0.0);
            }
        }.runTaskTimer(plugin, 0L, 10L);

        return true;
    }
}
