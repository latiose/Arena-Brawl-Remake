package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
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
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bouncing projectile. It passes through enemies (hitting each one once) and
 * reflects off blocks, which makes it strong in multi-level arenas and around
 * cover.
 */
public class Ricochet implements Ability {

    private static final double TRAIL_STEP = 0.5;

    private final double energyCost;
    private final double damage;
    private final int maxBounces;
    private final double speed;
    private final int maxLifetimeTicks;
    private final double hitRadius;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public Ricochet(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                    CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 40.0);
        this.damage = config.getDouble("damage", 70.0);
        this.maxBounces = config.getInt("max-bounces", 3);
        this.speed = config.getDouble("speed", 1.2);
        this.maxLifetimeTicks = config.getInt("max-lifetime-ticks", 40);
        this.hitRadius = config.getDouble("hit-radius", 0.8);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Ricochet";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location start = player.getEyeLocation();
        Vector initialDirection = start.getDirection();
        if (initialDirection.lengthSquared() < 0.0001) return false;
        initialDirection.normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.4f);

        new BukkitRunnable() {
            private final Set<UUID> hitPlayers = new HashSet<>();
            private Location position = start.clone();
            private Vector direction = initialDirection.clone();
            private int bounces = 0;
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= maxLifetimeTicks
                        || !player.isOnline()
                        || player.isDead()) {
                    poof(position.getWorld(), position);
                    cancel();
                    return;
                }

                World world = position.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                Vector delta = direction.clone().multiply(speed);
                double length = delta.length();

                RayTraceResult hit = world.rayTrace(
                        position, direction, length,
                        FluidCollisionMode.NEVER,
                        true,
                        hitRadius,
                        entity -> entity instanceof Player target
                                && !target.equals(player)
                                && !target.isDead()
                                && target.getGameMode() != GameMode.SPECTATOR
                                && teamManager.isEnemy(player, target)
                                && !hitPlayers.contains(target.getUniqueId())
                );

                if (hit == null) {
                    spawnTrail(world, position, direction, length);
                    position.add(delta);
                    return;
                }

                if (hit.getHitEntity() instanceof Player target) {
                    if (hitPlayers.add(target.getUniqueId())) {
                        combatService.applyAbilityDamage(
                                player, target, damage, getName(),
                                hit.getHitPosition().toLocation(world)
                        );
                        world.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 1.4f);
                        world.spawnParticle(
                                Particle.CRIT, hit.getHitPosition().toLocation(world),
                                14, 0.25, 0.25, 0.25, 0.1
                        );
                    }
                    position = hit.getHitPosition().toLocation(world)
                            .add(direction.clone().multiply(0.15));
                    return;
                }

                BlockFace face = hit.getHitBlockFace();
                if (face == null) {
                    poof(world, hit.getHitPosition().toLocation(world));
                    cancel();
                    return;
                }

                bounces++;
                // A bounce refreshes the projectile: enemies already hit can be struck again.
                hitPlayers.clear();
                Vector normal = face.getDirection();
                direction = direction.clone()
                        .subtract(normal.clone().multiply(2 * direction.dot(normal)))
                        .normalize();

                position = hit.getHitPosition().toLocation(world)
                        .add(normal.clone().multiply(0.15));

                world.playSound(position, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.6f);
                world.spawnParticle(
                        Particle.END_ROD, position, 6, 0.1, 0.1, 0.1, 0.02
                );

                if (bounces > maxBounces) {
                    poof(world, position);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void spawnTrail(World world, Location from, Vector direction, double length) {
        for (double distance = 0; distance < length; distance += TRAIL_STEP) {
            Location point = from.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.CRIT, point, 1, 0.03, 0.03, 0.03, 0.0);
            world.spawnParticle(Particle.END_ROD, point, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    private void poof(World world, Location location) {
        if (world == null || location == null) return;
        world.spawnParticle(Particle.POOF, location, 8, 0.15, 0.15, 0.15, 0.01);
    }

    @Override
    public String getDescription() {
        return "Fires a ricocheting projectile that bounces off walls up to "
                + maxBounces + " times. It passes through enemies and hits each one once per "
                + "bounce, so a ricochet can strike the same target again.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) damage + " HP"),
                new AbilityStat("Bounces", String.valueOf(maxBounces)),
                new AbilityStat("Max hits", "1 per enemy, per bounce"),
                new AbilityStat("Energy", (int) energyCost + "")
        );
    }
}
