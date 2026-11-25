package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Duskbringer implements Ability {

    private static final double PATH_STEP = 0.5;
    private static final double TRAIL_SPACING = 1.0;

    private final Plugin plugin;
    private final double energyCost;
    private final double damage;
    private final double range;
    private final double speed;
    private final double hitRadius;
    private final long trailDurationMs;
    private final double trailRadius;
    private final int speedAmplifier;
    private final long speedBuffMs;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final SpeedBuffManager speedBuffManager;

    private BukkitRunnable trailTask;

    public Duskbringer(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                       CombatService combatService, SpeedBuffManager speedBuffManager,
                       AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.damage = config.getDouble("damage", 90.0);
        this.range = config.getDouble("range", 20.0);
        this.speed = config.getDouble("speed", 0.9);
        this.hitRadius = config.getDouble("hit-radius", 1.6);
        this.trailDurationMs = config.getLong("trail-duration-ms", 5000L);
        this.trailRadius = config.getDouble("trail-radius", 2.0);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.speedBuffMs = config.getLong("speed-buff-ms", 600L);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.speedBuffManager = speedBuffManager;
    }

    @Override
    public String getName() {
        return "Duskbringer";
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

        final Location start = player.getLocation().clone();
        final Vector aim = direction.clone();
        final double pathLength = computePathLength(start, aim);

        if (trailTask != null) {
            trailTask.cancel();
            trailTask = null;
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        final long trailTicks = Math.max(1L, trailDurationMs / 50L);

        trailTask = new BukkitRunnable() {
            double bladeDistance = 0;
            long ticks = 0;
            final Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (ticks >= trailTicks || !player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR) {
                    trailTask = null;
                    cancel();
                    return;
                }

                ticks++;

                if (bladeDistance < pathLength) {
                    bladeDistance = Math.min(pathLength, bladeDistance + speed);
                    Location blade = start.clone().add(aim.clone().multiply(bladeDistance)).add(0, 1.0, 0);
                    drawBlade(blade);
                    damageEnemies(player, blade, hit);
                }

                drawTrail(start, aim, bladeDistance);

                if (isOnTrail(player, start, aim, bladeDistance)) {
                    speedBuffManager.applyBuff(player, speedAmplifier, speedBuffMs, getName());
                }
            }
        };
        trailTask.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private double computePathLength(Location start, Vector direction) {
        Location point = start.clone();
        double traveled = 0;

        while (traveled < range) {
            Location next = point.clone().add(direction.clone().multiply(PATH_STEP));
            point = next;
            traveled += PATH_STEP;
        }

        return traveled;
    }



    private void damageEnemies(Player caster, Location blade, Set<UUID> hit) {
        double radiusSquared = hitRadius * hitRadius;

        for (Entity entity : blade.getWorld().getNearbyEntities(blade, hitRadius, 2.5, hitRadius)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(caster) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (hit.contains(target.getUniqueId())) continue;
            if (target.getLocation().add(0, 1.0, 0).distanceSquared(blade) > radiusSquared) continue;

            hit.add(target.getUniqueId());
            combatService.applyAbilityDamage(caster, target, damage, getName(), blade);

            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0),
                    12, 0.3, 0.4, 0.3, 0.15);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.2f);
        }
    }

    private void drawBlade(Location blade) {
        blade.getWorld().spawnParticle(Particle.DUST, blade, 6, 0.2, 0.2, 0.2, 0.0,
                new Particle.DustOptions(Color.fromRGB(70, 20, 110), 1.6f));
        blade.getWorld().spawnParticle(Particle.SOUL, blade, 2, 0.15, 0.15, 0.15, 0.02);
        blade.getWorld().spawnParticle(Particle.SCULK_SOUL, blade, 1, 0.1, 0.1, 0.1, 0.01);
    }

    private void drawTrail(Location start, Vector direction, double revealed) {
        Vector side = new Vector(-direction.getZ(), 0, direction.getX());
        int index = 0;

        for (double distance = 0; distance <= revealed; distance += TRAIL_SPACING, index++) {
            Location point = start.clone().add(direction.clone().multiply(distance)).add(0, 0.15, 0);
            point.getWorld().spawnParticle(Particle.SCULK_SOUL, point, 1, 0.1, 0.02, 0.1, 0.0);

            if (index % 2 == 0) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    Location edge = point.clone().add(side.clone().multiply(trailRadius * 0.55 * sign));
                    edge.getWorld().spawnParticle(Particle.SMOKE, edge, 1, 0.05, 0.02, 0.05, 0.0);
                }
            }
        }
    }

    private boolean isOnTrail(Player player, Location start, Vector direction, double revealed) {
        Location location = player.getLocation();
        if (Math.abs(location.getY() - start.getY()) > 3.0) return false;

        double relX = location.getX() - start.getX();
        double relZ = location.getZ() - start.getZ();
        double along = relX * direction.getX() + relZ * direction.getZ();

        if (along < -trailRadius || along > revealed + trailRadius) return false;

        double clamped = Math.max(0, Math.min(revealed, along));
        double closestX = start.getX() + direction.getX() * clamped;
        double closestZ = start.getZ() + direction.getZ() * clamped;
        double distX = location.getX() - closestX;
        double distZ = location.getZ() - closestZ;

        return distX * distX + distZ * distZ <= trailRadius * trailRadius;
    }

    @Override
    public String getDescription() {
        return "Throws a shadow blade in a straight line, damaging every enemy it passes through and "
                + "leaving a shadow trail for " + (trailDurationMs / 1000L) + "s. While standing on the "
                + "trail you gain Speed " + roman(speedAmplifier + 1) + ".";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Trail Duration", (trailDurationMs / 1000L) + "s"),
                new AbilityStat("Trail Speed Bonus", "Speed " + roman(speedAmplifier + 1)),
                new AbilityStat("Range", (int) range + " blocks"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
