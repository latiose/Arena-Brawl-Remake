package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.bukkit.util.BoundingBox;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class CosmicBinding implements Ability {
    private static final double WALL_GRACE = 4.0;
    private final Plugin plugin;
    private final double energyCost;
    private final double damage;
    private final double maxRange;
    private final double projectileSpeed;
    private final double hitRadius;
    private final long slowDurationMs;
    private final long stunDurationMs;
    private final double aoeRadius;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;

    public CosmicBinding(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                         CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.damage = config.getDouble("damage", 80.0);
        this.maxRange = config.getDouble("max-range", 18.0);
        this.projectileSpeed = config.getDouble("projectile-speed", 0.7);
        this.hitRadius = config.getDouble("hit-radius", 0.4);
        this.slowDurationMs = config.getLong("slow-duration-ms", 1000L);
        this.stunDurationMs = config.getLong("stun-duration-ms", 1300L);
        this.aoeRadius = config.getDouble("aoe-radius", 2.0);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() {
        return "Cosmic Binding";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getEyeLocation().getDirection().normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.4f);

        new BukkitRunnable() {
            final Location current = player.getEyeLocation().clone();
            double traveled = 0;
            double hitAt = -1;
            Player firstTarget = null;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR) {
                    cancel();
                    return;
                }

                RayTraceResult entityHit = current.getWorld().rayTrace(
                        current, direction, projectileSpeed, FluidCollisionMode.NEVER, true, hitRadius,
                        entity -> entity instanceof Player candidate
                                && !candidate.equals(player)
                                && !candidate.equals(firstTarget)
                                && !candidate.isDead()
                                && candidate.getGameMode() != GameMode.SPECTATOR
                                && teamManager.isEnemy(player, candidate));
                RayTraceResult blockHit = current.getWorld().rayTraceBlocks(
                        current, direction, projectileSpeed, FluidCollisionMode.NEVER, true);

                double entityDistance = entityHit != null
                        ? entityHit.getHitPosition().distance(current.toVector()) : Double.MAX_VALUE;
                double blockDistance = blockHit != null
                        ? blockHit.getHitPosition().distance(current.toVector()) : Double.MAX_VALUE;

                if (entityHit != null && entityDistance <= blockDistance
                        && entityHit.getHitEntity() instanceof Player target) {

                    hit(player, target, current);

                    if (firstTarget == null) {
                        firstTarget = target;
                        hitAt = traveled + entityDistance;
                        debuffManager.tryApply(player, target, DebuffType.SLOW, slowDurationMs);

                        if (isWallBehind(target, direction)
                                ||(isWall(blockHit) && Math.abs(direction.getY()) <= MAX_WALL_PITCH
                                && (blockDistance - entityDistance) <= WALL_GRACE)) {
                            stun(player, firstTarget);
                            cancel();
                            return;
                        }
                    } else {
                        stun(player, firstTarget);
                        stun(player, target);
                        cancel();
                        return;
                    }
                } else if (blockHit != null) {
                    if (firstTarget == null) {
                        Location impact = blockHit.getHitPosition().toLocation(current.getWorld());
                        List<Player> victims = findAoeTargets(player, impact);

                        if (!victims.isEmpty()) {
                            Player first = victims.get(0);
                            hit(player, first, impact);
                            debuffManager.tryApply(player, first, DebuffType.SLOW, slowDurationMs);

                            if (victims.size() > 1) {
                                Player second = victims.get(1);
                                hit(player, second, impact);
                                stun(player, first);
                                stun(player, second);
                            } else if (isWall(blockHit) && Math.abs(direction.getY()) <= MAX_WALL_PITCH) {
                                stun(player, first);
                            }

                            drawBurst(impact);
                            cancel();
                            return;
                        }
                    }
                    cancel();
                    return;
                }

                current.add(direction.clone().multiply(projectileSpeed));
                traveled += projectileSpeed;
                drawTrail(current);

                if (traveled >= maxRange) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void hit(Player caster, Player target, Location impact) {
        combatService.applyAbilityDamage(caster, target, damage, getName(), impact);

        impact.getWorld().spawnParticle(Particle.FIREWORK, impact, 15, 0.2, 0.2, 0.2, 0.05);
        impact.getWorld().spawnParticle(Particle.END_ROD, impact, 8, 0.15, 0.15, 0.15, 0.02);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.5f);
    }

    private void stun(Player caster, Player target) {
        if (target == null || !target.isOnline() || target.isDead()) return;

        debuffManager.clearIfType(target, DebuffType.SLOW);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!target.isOnline() || target.isDead()) return;

            boolean applied = debuffManager.tryApply(caster, target, DebuffType.STUN, stunDurationMs);
            if (!applied) {
                plugin.getLogger().warning("Stun rechazado en " + target.getName());
                return;
            }

            Location loc = target.getLocation().add(0, 1.0, 0);
            target.getWorld().spawnParticle(Particle.ENCHANT, loc, 25, 0.35, 0.6, 0.35, 0.5);
            target.getWorld().spawnParticle(Particle.END_ROD, loc, 12, 0.25, 0.5, 0.25, 0.03);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
        });
    }

    private void drawTrail(Location point) {
        point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0.01);
        point.getWorld().spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05,
                new Particle.DustOptions(Color.fromRGB(255, 220, 130), 1.0f));
    }

    @Override
    public String getDescription() {
        return "Launches a cosmic binding that damages and slows the first enemy hit. "
                + "If it then hits a second enemy or a wall, both targets are stunned.";
    }
    private static final double MAX_WALL_PITCH = 0.5;
    private boolean isWallBehind(Player target, Vector direction) {
        // Steep up/down shots (about 30 degrees or more) never count as pushing into a wall
        if (Math.abs(direction.getY()) > MAX_WALL_PITCH) return false;

        Vector flat = direction.clone().setY(0);
        if (flat.lengthSquared() < 1e-6) return false;
        flat.normalize();

        World world = target.getWorld();
        for (double height : new double[]{0.3, 1.0, 1.7}) {
            Location origin = target.getLocation().add(0, height, 0);
            RayTraceResult result = world.rayTraceBlocks(origin, flat, WALL_GRACE, FluidCollisionMode.NEVER, true);
            if (isWall(result)) return true;
        }
        return false;
    }

    private boolean isWall(RayTraceResult result) {
        if (result == null || result.getHitBlockFace() == null) return false;
        return switch (result.getHitBlockFace()) {
            case NORTH, SOUTH, EAST, WEST -> true;
            default -> false;
        };
    }

    private List<Player> findAoeTargets(Player caster, Location impact) {
        List<Player> found = new ArrayList<>();
        Vector point = impact.toVector();

        for (Player candidate : impact.getWorld().getPlayers()) {
            if (candidate.equals(caster) || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, candidate)) continue;
            if (distanceToBox(candidate.getBoundingBox(), point) <= aoeRadius) {
                found.add(candidate);
            }
        }

        found.sort(Comparator.comparingDouble(p -> distanceToBox(p.getBoundingBox(), point)));
        return found;
    }

    /** Distance from a point to the closest point on a bounding box (0 if inside). */
    private double distanceToBox(BoundingBox box, Vector p) {
        double dx = Math.max(Math.max(box.getMinX() - p.getX(), 0), p.getX() - box.getMaxX());
        double dy = Math.max(Math.max(box.getMinY() - p.getY(), 0), p.getY() - box.getMaxY());
        double dz = Math.max(Math.max(box.getMinZ() - p.getZ(), 0), p.getZ() - box.getMaxZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void drawBurst(Location impact) {
        World world = impact.getWorld();
        world.spawnParticle(Particle.FIREWORK, impact, 25, aoeRadius * 0.4, 0.2, aoeRadius * 0.4, 0.05);
        world.spawnParticle(Particle.END_ROD, impact, 15, aoeRadius * 0.4, 0.2, aoeRadius * 0.4, 0.03);

        int points = 16;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i;
            world.spawnParticle(Particle.DUST,
                    impact.clone().add(Math.cos(angle) * aoeRadius, 0.1, Math.sin(angle) * aoeRadius),
                    1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(255, 220, 130), 1.0f));
        }
        world.playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.5f);
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Slow", (slowDurationMs / 1000.0) + "s"),
                new AbilityStat("Stun", (stunDurationMs / 1000.0) + "s"),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Impact Radius", aoeRadius + " blocks")
        );
    }
}
