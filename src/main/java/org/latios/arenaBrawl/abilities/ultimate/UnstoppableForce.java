
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class UnstoppableForce implements Ability {

    private final Plugin plugin;
    private final double maxRange;
    private final double impactRadius;
    private final double damage;
    private final long stunDurationMs;
    private final int dashTicks;
    private final long chargeTimeMillis;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;

    private static final Color ROCK_COLOR = Color.fromRGB(100, 100, 110);
    private static final Color DARK_ROCK = Color.fromRGB(65, 65, 75);

    public UnstoppableForce(
            Plugin plugin,
            CooldownManager cooldownManager,
            UsageManager usageManager,
            TeamManager teamManager,
            CombatService combatService,
            DebuffManager debuffManager,
            AbilityConfig config
    ) {
        this.plugin = plugin;

        this.maxRange = config.getDouble("max-range", 22.0);
        this.impactRadius = config.getDouble("impact-radius", 5.0);
        this.damage = config.getDouble("damage", 350.0);
        this.stunDurationMs = config.getLong("stun-duration-ms", 1500L);
        this.dashTicks = Math.max(1, config.getInt("dash-ticks", 5));
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(
                cooldownManager, usageManager, "unstoppableforce"
        );

        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() {
        return "Unstoppable Force";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(
                player, "unstoppableforce", chargeTimeMillis
        );
    }

    @Override
    public boolean activate(Player player) {
        final Location start = player.getLocation().clone();
        final Location eye = player.getEyeLocation();

        final Location destination = safeDestination(resolveDestination(player));
        destination.setDirection(start.getDirection());

        World world = player.getWorld();

        // Initial launch sounds
        world.playSound(start, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.45f);
        world.playSound(start, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
        world.playSound(start, Sound.BLOCK_STONE_BREAK, 1.3f, 0.6f);

        // Takeoff explosion
        world.spawnParticle(
                Particle.EXPLOSION,
                start.clone().add(0, 0.8, 0),
                2,
                0.4, 0.5, 0.4,
                0
        );

        world.spawnParticle(
                Particle.BLOCK,
                start.clone().add(0, 0.5, 0),
                45,
                0.6, 0.5, 0.6,
                0.25,
                Material.STONE.createBlockData()
        );

        world.spawnParticle(
                Particle.CLOUD,
                start.clone().add(0, 0.5, 0),
                25,
                0.6, 0.4, 0.6,
                0.12
        );

        // Dash animation
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()
                        || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR) {
                    cancel();
                    return;
                }

                tick++;

                double progress = Math.min(1.0, (double) tick / dashTicks);

                Location point = start.clone().add(
                        destination.toVector()
                                .subtract(start.toVector())
                                .multiply(progress)
                );

                point.setDirection(destination.getDirection());

                player.teleport(point);
                player.setFallDistance(0);

                drawTrail(point, tick);

                // Mid-dash sonic boom
                if (tick == Math.max(1, dashTicks / 2)) {
                    world.playSound(
                            point,
                            Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                            1.1f,
                            0.5f
                    );

                    world.spawnParticle(
                            Particle.CLOUD,
                            point,
                            20,
                            0.6, 0.5, 0.6,
                            0.15
                    );
                }

                if (tick >= dashTicks) {
                    player.teleport(destination);
                    player.setFallDistance(0);

                    impact(player, destination);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Location resolveDestination(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        RayTraceResult result = player.getWorld().rayTraceBlocks(
                eye,
                direction,
                maxRange
        );

        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition()
                    .toLocation(player.getWorld())
                    .subtract(direction.clone().multiply(0.8));
        }

        return eye.clone().add(direction.multiply(maxRange));
    }

    private Location safeDestination(Location base) {
        Location destination = base.clone();

        // Prevent landing inside solid blocks
        for (int i = 0; i < 5 && isBlocked(destination); i++) {
            destination.add(0, 1, 0);
        }

        return destination;
    }

    private boolean isBlocked(Location location) {
        return location.getBlock().getType().isSolid()
                || location.clone().add(0, 1, 0)
                .getBlock().getType().isSolid();
    }

    private void drawTrail(Location point, int tick) {
        World world = point.getWorld();

        Location center = point.clone().add(0, 1.0, 0);

        // Thick grey dust trail
        world.spawnParticle(
                Particle.DUST,
                center,
                12,
                0.45, 0.5, 0.45,
                0,
                new Particle.DustOptions(ROCK_COLOR, 2.2f)
        );

        // Stone fragments
        world.spawnParticle(
                Particle.BLOCK,
                center,
                8,
                0.5, 0.5, 0.5,
                0.2,
                Material.STONE.createBlockData()
        );

        // Darker rock fragments
        world.spawnParticle(
                Particle.BLOCK,
                center,
                5,
                0.3, 0.4, 0.3,
                0.15,
                Material.COBBLESTONE.createBlockData()
        );

        // Smoke trail
        world.spawnParticle(
                Particle.CLOUD,
                center,
                6,
                0.3, 0.3, 0.3,
                0.08
        );

        // Critical sparks
        world.spawnParticle(
                Particle.CRIT,
                center,
                5,
                0.3, 0.3, 0.3,
                0.15
        );

        // Additional sonic streaks
        if (tick % 2 == 0) {
            world.spawnParticle(
                    Particle.SWEEP_ATTACK,
                    center,
                    2,
                    0.2, 0.2, 0.2,
                    0
            );
        }
    }

    private void impact(Player caster, Location center) {
        World world = center.getWorld();

        // Main impact sounds
        world.playSound(
                center,
                Sound.ENTITY_GENERIC_EXPLODE,
                2.0f,
                0.55f
        );

        world.playSound(
                center,
                Sound.ENTITY_IRON_GOLEM_DEATH,
                1.8f,
                0.65f
        );

        world.playSound(
                center,
                Sound.BLOCK_STONE_BREAK,
                2.0f,
                0.45f
        );

        world.playSound(
                center,
                Sound.ENTITY_WARDEN_SONIC_BOOM,
                1.0f,
                0.7f
        );

        // Massive central explosion
        world.spawnParticle(
                Particle.EXPLOSION,
                center.clone().add(0, 0.5, 0),
                8,
                1.0, 0.4, 1.0,
                0
        );

        // Huge cloud of stone debris
        world.spawnParticle(
                Particle.BLOCK,
                center.clone().add(0, 0.5, 0),
                120,
                impactRadius * 0.5,
                0.8,
                impactRadius * 0.5,
                0.35,
                Material.STONE.createBlockData()
        );

        world.spawnParticle(
                Particle.BLOCK,
                center.clone().add(0, 0.5, 0),
                70,
                impactRadius * 0.4,
                0.7,
                impactRadius * 0.4,
                0.3,
                Material.COBBLESTONE.createBlockData()
        );

        // Dust blast
        world.spawnParticle(
                Particle.DUST,
                center.clone().add(0, 0.5, 0),
                100,
                impactRadius * 0.5,
                0.5,
                impactRadius * 0.5,
                0,
                new Particle.DustOptions(ROCK_COLOR, 2.5f)
        );

        world.spawnParticle(
                Particle.DUST,
                center.clone().add(0, 0.5, 0),
                70,
                impactRadius * 0.4,
                0.4,
                impactRadius * 0.4,
                0,
                new Particle.DustOptions(DARK_ROCK, 2.0f)
        );

        world.spawnParticle(
                Particle.CLOUD,
                center.clone().add(0, 0.3, 0),
                60,
                impactRadius * 0.45,
                0.3,
                impactRadius * 0.45,
                0.15
        );

        world.spawnParticle(
                Particle.CRIT,
                center.clone().add(0, 0.5, 0),
                50,
                impactRadius * 0.4,
                0.5,
                impactRadius * 0.4,
                0.3
        );

        // Expanding ground shockwaves
        createShockwaves(center);

        // Damage and knockback
        for (Entity nearby : world.getNearbyEntities(
                center,
                impactRadius,
                impactRadius,
                impactRadius
        )) {
            if (!(nearby instanceof Player target)) continue;
            if (target.equals(caster) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;

            if (target.getLocation().distanceSquared(center)
                    > impactRadius * impactRadius) continue;

            // Stun
            debuffManager.tryApply(
                    caster,
                    target,
                    DebuffType.STUN,
                    stunDurationMs
            );

            // Damage
            combatService.applyAbilityDamage(
                    caster,
                    target,
                    damage,
                    getName(),
                    center
            );

            // Knock enemies into the air
            Vector knockback = target.getLocation()
                    .toVector()
                    .subtract(center.toVector());

            knockback.setY(0);

            if (knockback.lengthSquared() < 0.01) {
                knockback = caster.getLocation()
                        .getDirection()
                        .setY(0);
            }

            if (knockback.lengthSquared() > 0) {
                knockback.normalize().multiply(1.5);
            }

            knockback.setY(0.85);

            target.setVelocity(knockback);

            // Hit effects
            Location targetCenter = target.getLocation()
                    .add(0, 1, 0);

            world.spawnParticle(
                    Particle.EXPLOSION,
                    targetCenter,
                    1,
                    0.2, 0.2, 0.2,
                    0
            );

            world.spawnParticle(
                    Particle.CRIT,
                    targetCenter,
                    25,
                    0.4, 0.7, 0.4,
                    0.25
            );

            world.spawnParticle(
                    Particle.BLOCK,
                    targetCenter,
                    20,
                    0.4, 0.5, 0.4,
                    0.2,
                    Material.STONE.createBlockData()
            );

            target.getWorld().playSound(
                    target.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                    1.5f,
                    0.6f
            );

            target.getWorld().playSound(
                    target.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_CRIT,
                    1.0f,
                    0.7f
            );
        }
    }

    /**
     * Creates several expanding rings of dust and debris.
     */
    private void createShockwaves(Location center) {
        new BukkitRunnable() {
            int tick = 0;
            final int duration = 7;

            @Override
            public void run() {
                if (tick >= duration) {
                    cancel();
                    return;
                }

                double radius = impactRadius * ((tick + 1.0) / duration);
                int points = Math.max(24, (int) (radius * 16));

                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLocation = center.clone().add(x, 0.15, z);

                    // Dust ring
                    center.getWorld().spawnParticle(
                            Particle.DUST,
                            particleLocation,
                            1,
                            0.03, 0.05, 0.03,
                            0,
                            new Particle.DustOptions(ROCK_COLOR, 2.0f)
                    );

                    // Debris ring
                    if (i % 3 == 0) {
                        center.getWorld().spawnParticle(
                                Particle.BLOCK,
                                particleLocation,
                                1,
                                0.05, 0.1, 0.05,
                                0.2,
                                Material.STONE.createBlockData()
                        );
                    }

                    // Outer energy ring
                    if (i % 5 == 0) {
                        center.getWorld().spawnParticle(
                                Particle.CRIT,
                                particleLocation.clone().add(0, 0.4, 0),
                                1,
                                0.05, 0.1, 0.05,
                                0.1
                        );
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public String getDescription() {
        return "Launches forward like a living mountain, smashing into the ground "
                + "with a devastating shockwave that damages, stuns, and launches enemies.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Stun", (stunDurationMs / 1000.0) + "s"),
                new AbilityStat("Impact Radius", (int) impactRadius + " blocks"),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}
