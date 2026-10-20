package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Individualistic support channel. It drains an enemy in front of the caster
 * and heals the caster, without needing an ally to be nearby or alive.
 */
public class VitalSiphon implements Ability {

    private final long cooldownMs;
    private final double maxRange;
    private final long durationMs;
    private final double damagePerSecond;
    private final double healPerSecond;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final CombatService combatService;
    private final AbilityConfig config;

    public VitalSiphon(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                       PlayerHealthManager healthManager, CombatService combatService,
                       CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.maxRange = config.getDouble("max-range", 16.0);
        this.durationMs = config.getLong("duration-ms", 4000L);
        this.damagePerSecond = config.getDouble("damage-per-second", 50.0);
        this.healPerSecond = config.getDouble("heal-per-second", 65.0);

        this.cost = new CooldownCost(cooldownManager, "vitalsiphon", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Vital Siphon";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player target = findTarget(player);
        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.6f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.8f);

        final int durationTicks = Math.max(20, (int) (durationMs / 50L));
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks
                        || !player.isOnline() || !target.isOnline()
                        || player.isDead() || target.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || target.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)
                        || !config.getMatchManager().isInMatch(target)
                        || player.getLocation().distanceSquared(target.getLocation()) > maxRange * maxRange
                        || !player.hasLineOfSight(target)) {
                    cancel();
                    return;
                }

                drawBeam(player.getEyeLocation(), target.getLocation().add(0, 1.0, 0));

                if (ticks % 20 == 0) {
                    combatService.applyAbilityDamage(
                            player, target, damagePerSecond, getName(),
                            target.getLocation().add(0, target.getHeight() * 0.5, 0)
                    );
                    if (healthManager.getHealth(player) < healthManager.getMaxHealth(player)) {
                        healthManager.heal(player, healPerSecond, getName());
                    }

                    player.getWorld().spawnParticle(
                            Particle.HEART, player.getLocation().add(0, 1.0, 0), 4,
                            0.35, 0.45, 0.35, 0.02
                    );
                    target.getWorld().spawnParticle(
                            Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.0, 0), 10,
                            0.3, 0.45, 0.3, 0.05
                    );
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private Player findTarget(Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTrace(
                eye, eye.getDirection().normalize(), maxRange,
                FluidCollisionMode.NEVER, true, 0.45,
                entity -> entity instanceof Player candidate
                        && !candidate.equals(player)
                        && !candidate.isDead()
                        && candidate.getGameMode() != GameMode.SPECTATOR
                        && teamManager.isEnemy(player, candidate)
        );

        return hit != null && hit.getHitEntity() instanceof Player target ? target : null;
    }

    private void drawBeam(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length <= 0) return;

        direction.normalize().multiply(0.4);
        Location current = start.clone();
        double traveled = 0;
        while (traveled < length) {
            current.getWorld().spawnParticle(
                    Particle.DUST, current, 1, 0.0, 0.0, 0.0,
                    new Particle.DustOptions(Color.fromRGB(220, 30, 60), 1.1f)
            );
            current.add(direction);
            traveled += 0.4;
        }
    }

    @Override
    public String getDescription() {
        return "Channels a life-draining beam for " + (durationMs / 1000L) +
                "s. Deals " + (int) damagePerSecond + " damage per second and heals you for " +
                (int) healPerSecond + " HP per second. Breaks if line of sight is lost.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Range", String.format("%.1f blocks", maxRange)),
                new AbilityStat("Damage", (int) damagePerSecond + " /s"),
                new AbilityStat("Self Heal", (int) healPerSecond + " /s"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s")
        );
    }
}
