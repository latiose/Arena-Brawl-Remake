package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PhantomUndertow implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final double dashSpeed;
    private final long dashDurationMs;
    private final long returnDelayMs;
    private final double phantomSpeed;
    private final double hitRadius;
    private final long stunDurationMs;
    private final long returnTimeoutMs;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    public PhantomUndertow(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                           DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager,
                           AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 18000L);
        this.dashSpeed = config.getDouble("dash-speed", 1.2);
        this.dashDurationMs = config.getLong("dash-duration-ms", 250L);
        this.returnDelayMs = config.getLong("return-delay-ms", 1000L);
        this.phantomSpeed = config.getDouble("phantom-speed", 1.0);
        this.hitRadius = config.getDouble("hit-radius", 1.5);
        this.stunDurationMs = config.getLong("stun-duration-ms", 800L);
        this.returnTimeoutMs = config.getLong("return-timeout-ms", 3000L);

        this.cost = new CooldownCost(cooldownManager, "phantomundertow", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() {
        return "Phantom Undertow";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 1e-6) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();

        Location origin = player.getLocation().clone();
        Vector dashVelocity = direction.clone().multiply(dashSpeed);
        long dashTicks = Math.max(1L, dashDurationMs / 50L);

        drawPhantom(origin);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.3f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= dashTicks) {
                    cancel();
                    if (player.isOnline() && !player.isDead()) {
                        scheduleReturn(player, origin);
                    }
                    return;
                }

                player.setVelocity(dashVelocity);
                player.setFallDistance(0);

                Location trail = player.getLocation().add(0, 1.0, 0);
                trail.getWorld().spawnParticle(Particle.SOUL, trail, 3, 0.15, 0.2, 0.15, 0.01);
                trail.getWorld().spawnParticle(Particle.DUST, trail, 2, 0.1, 0.15, 0.1,
                        new Particle.DustOptions(Color.fromRGB(70, 220, 200), 1.0f));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void scheduleReturn(Player caster, Location origin) {
        long delayTicks = Math.max(0L, returnDelayMs / 50L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!caster.isOnline() || caster.isDead()) return;
            startReturn(caster, origin);
        }, delayTicks);
    }

    private void startReturn(Player caster, Location origin) {
        Location phantom = origin.clone();
        Set<UUID> hit = new HashSet<>();
        long timeoutTicks = Math.max(1L, returnTimeoutMs / 50L);

        caster.getWorld().playSound(phantom, Sound.ENTITY_PHANTOM_SWOOP, 1.0f, 1.2f);

        new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || caster.isDead()
                        || caster.getGameMode() == GameMode.SPECTATOR || ticks >= timeoutTicks) {
                    cancel();
                    return;
                }

                Location casterLocation = caster.getLocation();
                Vector toCaster = casterLocation.toVector().subtract(phantom.toVector());
                double distance = toCaster.length();

                if (distance <= phantomSpeed) {
                    drawPhantom(casterLocation.clone());
                    cancel();
                    return;
                }

                phantom.add(toCaster.normalize().multiply(phantomSpeed));
                drawPhantom(phantom);
                stunNearby(caster, phantom, hit);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void stunNearby(Player caster, Location phantom, Set<UUID> hit) {
        Location center = phantom.clone().add(0, 1.0, 0);

        for (Player candidate : center.getWorld().getPlayers()) {
            if (candidate.equals(caster) || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, candidate)) continue;
            if (hit.contains(candidate.getUniqueId())) continue;

            if (candidate.getLocation().add(0, 1.0, 0).distanceSquared(center) <= hitRadius * hitRadius) {
                hit.add(candidate.getUniqueId());
                debuffManager.tryApply(caster, candidate, DebuffType.STUN, stunDurationMs);

                candidate.getWorld().spawnParticle(Particle.SOUL, candidate.getLocation().add(0, 1.0, 0),
                        15, 0.3, 0.4, 0.3, 0.02);
                candidate.getWorld().spawnParticle(Particle.END_ROD, candidate.getLocation().add(0, 1.0, 0),
                        8, 0.2, 0.4, 0.2, 0.02);
                candidate.getWorld().playSound(candidate.getLocation(), Sound.ENTITY_PHANTOM_HURT, 0.8f, 1.4f);
            }
        }
    }

    private void drawPhantom(Location location) {
        Location center = location.clone().add(0, 1.0, 0);
        center.getWorld().spawnParticle(Particle.SOUL, center, 6, 0.25, 0.35, 0.25, 0.01);
        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 3, 0.2, 0.3, 0.2, 0.01);
        center.getWorld().spawnParticle(Particle.DUST, center, 3, 0.2, 0.3, 0.2,
                new Particle.DustOptions(Color.fromRGB(60, 200, 190), 1.0f));
    }

    @Override
    public String getDescription() {
        return "Dashes forward and leaves a phantom behind. After a short delay the phantom returns to you, "
                + "stunning the enemies it passes through.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Stun", (stunDurationMs / 1000.0) + "s"),
                new AbilityStat("Return Delay", (returnDelayMs / 1000.0) + "s"),
                new AbilityStat("Hit Radius", String.format("%.1f blocks", hitRadius)),
                new AbilityStat("Damage", "None"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
