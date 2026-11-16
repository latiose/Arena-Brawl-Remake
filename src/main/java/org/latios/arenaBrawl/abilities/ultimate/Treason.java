package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Treason implements Ability {

    private record Mark(UUID casterId, long expiresAt, double percent) {}

    private static final Map<UUID, Mark> ACTIVE = new HashMap<>();

    private final double maxRange;
    private final long durationMillis;
    private final double redirectPercent;
    private final long chargeTimeMillis;
    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final AbilityCost cost;

    public Treason(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                   TeamManager teamManager, DebuffManager debuffManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;

        this.maxRange = config.getDouble("max-range", 20.0);
        this.durationMillis = config.getLong("duration-ms", 4000L);
        this.redirectPercent = config.getDouble("redirect-percent", 0.50);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);

        this.cost = new UltimateCost(cooldownManager, usageManager, "treason");
    }

    @Override
    public String getName() {
        return "Treason";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "treason", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Marks an enemy. For a short time, half of the damage they deal is redirected to their own ally and their healing is blocked.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Damage Redirected", (int) (redirectPercent * 100) + "%"),
                new AbilityStat("Healing", "Blocked"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        ACTIVE.put(target.getUniqueId(), new Mark(player.getUniqueId(), System.currentTimeMillis() + durationMillis, redirectPercent));
        debuffManager.tryApply(player, target, DebuffType.ANTIHEAL, durationMillis, 100.0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 10.0f, 0.7f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10.0f, 1.8f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.0, 0), 40, 0.4, 0.8, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(190, 0, 30), 1.4f));

        target.sendMessage("§eTreason§e marked you! Your healing is blocked and your damage is being redirected.");
        player.sendMessage("§eYou marked " + target.getName() + "§e with §eTreason§e!");

        startMarkParticles(target);

        return true;
    }

    private void startMarkParticles(Player target) {
        long durationTicks = durationMillis / 50L;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                Mark mark = ACTIVE.get(target.getUniqueId());
                if (mark == null || !target.isOnline() || target.isDead() || ticks >= durationTicks) {
                    cancel();
                    return;
                }

                target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.0, 0), 4, 0.3, 0.6, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(190, 0, 30), 1.1f));
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public static double redirectDamage(Player attacker, Player victim, double damage,
                                        TeamManager teamManager, CombatService combatService) {
        if (attacker == null || damage <= 0 || teamManager == null || combatService == null) {
            return damage;
        }

        Mark mark = ACTIVE.get(attacker.getUniqueId());
        if (mark == null) {
            return damage;
        }

        if (System.currentTimeMillis() >= mark.expiresAt()) {
            ACTIVE.remove(attacker.getUniqueId());
            return damage;
        }

        if (victim == null || !teamManager.isEnemy(attacker, victim)) {
            return damage;
        }

        Player ally = findNearestAlly(attacker, teamManager);
        if (ally == null) {
            return damage;
        }

        double redirected = damage * mark.percent();
        combatService.applyAbilityDamage(attacker, ally, redirected, "Treason");

        ally.getWorld().spawnParticle(Particle.DUST, ally.getLocation().add(0, 1.0, 0), 12, 0.3, 0.4, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(190, 0, 30), 1.2f));

        return damage - redirected;
    }

    private static Player findNearestAlly(Player attacker, TeamManager teamManager) {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Player candidate : attacker.getWorld().getPlayers()) {
            if (candidate.equals(attacker)) continue;
            if (candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isAlly(attacker, candidate)) continue;

            double distance = candidate.getLocation().distanceSquared(attacker.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }
}
