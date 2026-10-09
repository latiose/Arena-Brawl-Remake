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
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class SpikeGrenade implements Ability {

    private static final double TRAIL_SPACING = 0.5;
    private static final double SURFACE_OFFSET = 0.3;
    private static final double FLOOR_LIFT = 1.0;

    private final double energyCost;
    private final double grenadeDamage;
    private final double needleDamage;
    private final double maxRange;
    private final double grenadeSpeed;
    private final double grenadeHitRadius;
    private final int needleCount;
    private final double needleRange;
    private final double needleSpeed;
    private final double needleHitRadius;
    private final AbilityConfig config;
    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public SpikeGrenade(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                        CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 30.0);
        this.grenadeDamage = config.getDouble("grenade-damage", 60.0);
        this.needleDamage = config.getDouble("needle-damage", 5.0);
        this.maxRange = config.getDouble("max-range", 15.0);
        this.grenadeSpeed = config.getDouble("grenade-speed", 1.5);
        this.grenadeHitRadius = config.getDouble("grenade-hit-radius", 0.4);
        this.needleCount = config.getInt("needle-count", 6);
        this.needleRange = config.getDouble("needle-range", 6.0);
        this.needleSpeed = config.getDouble("needle-speed", 1.0);
        this.needleHitRadius = config.getDouble("needle-hit-radius", 0.3);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Spike Grenade";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        MatchSoundUtils.play(config, player, Sound.ENTITY_EGG_THROW, 1.2f, 0.7f);

        new BukkitRunnable() {
            private final Location pos = start.clone();
            private double traveled = 0;

            @Override
            public void run() {
                double segment = Math.min(grenadeSpeed, maxRange - traveled);
                RayTraceResult hit = trace(player, pos, direction, segment, grenadeHitRadius, null);

                double length = hit != null
                        ? Math.min(segment, hit.getHitPosition().distance(pos.toVector()))
                        : segment;
                spawnGrenadeTrail(pos, direction, length);

                if (hit != null) {
                    World world = pos.getWorld();
                    if (hit.getHitEntity() instanceof Player target) {
                        Location impact = target.getBoundingBox().getCenter().toLocation(world);
                        combatService.applyAbilityDamage(player, target, grenadeDamage, getName(), impact);
                        explode(player, impact, target, null);
                    } else {
                        BlockFace face = hit.getHitBlockFace();
                        Location impact = hit.getHitPosition().toLocation(world);
                        if (face != null) {
                            impact.add(face.getDirection().multiply(SURFACE_OFFSET));
                        }
                        explode(player, impact, null, face);
                    }
                    cancel();
                    return;
                }

                pos.add(direction.clone().multiply(segment));
                traveled += segment;

                if (traveled >= maxRange - 1.0E-6) {
                    explode(player, pos.clone(), null, null);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void explode(Player caster, Location center, Player directTarget, BlockFace face) {
        World world = center.getWorld();
        MatchSoundUtils.play(config, caster, Sound.ENTITY_ITEM_BREAK, 7f, 0.5f);
        MatchSoundUtils.play(config, caster, Sound.BLOCK_BONE_BLOCK_BREAK, 7f, 1.4f);

        world.spawnParticle(Particle.ITEM_SLIME, center, 35, 0.4, 0.4, 0.4, 0.15);
        world.spawnParticle(Particle.SCRAPE, center, 20, 0.3, 0.3, 0.3, 0.1);

        if (directTarget != null) {
            for (int i = 0; i < needleCount; i++) {
                if (directTarget.isDead()) break;
                combatService.applyAbilityDamage(caster, directTarget, needleDamage, getName(), center);
            }
        }
        Location origin = center.clone();
        if (face == BlockFace.UP) {
            Location lifted = center.clone().add(0, FLOOR_LIFT, 0);
            if (lifted.getBlock().isPassable()) {
                origin = lifted;
            }
        }

        double angleStep = 2 * Math.PI / needleCount;
        for (int i = 0; i < needleCount; i++) {
            double angle = i * angleStep;
            Vector needleDir = new Vector(Math.cos(angle), 0, Math.sin(angle));
            launchNeedle(caster, origin.clone(), needleDir, directTarget);
        }
    }

    private void launchNeedle(Player caster, Location start, Vector dir, Player ignored) {
        new BukkitRunnable() {
            private final Location pos = start.clone();
            private double traveled = 0;

            @Override
            public void run() {
                double segment = Math.min(needleSpeed, needleRange - traveled);
                RayTraceResult hit = trace(caster, pos, dir, segment, needleHitRadius, ignored);

                double length = hit != null
                        ? Math.min(segment, hit.getHitPosition().distance(pos.toVector()))
                        : segment;
                spawnNeedleTrail(pos, dir, length);

                if (hit != null) {
                    Location impact = hit.getHitPosition().toLocation(pos.getWorld());
                    if (hit.getHitEntity() instanceof Player enemy) {
                        combatService.applyAbilityDamage(caster, enemy, needleDamage, getName(), impact);
                        pos.getWorld().playSound(impact, Sound.ENTITY_PLAYER_HURT, 0.8f, 1.8f);
                        pos.getWorld().spawnParticle(Particle.CRIT, impact, 8, 0.2, 0.2, 0.2, 0.1);
                    } else {
                        pos.getWorld().spawnParticle(Particle.CRIT, impact, 5, 0.1, 0.1, 0.1, 0.05);
                    }
                    cancel();
                    return;
                }

                pos.add(dir.clone().multiply(segment));
                traveled += segment;

                if (traveled >= needleRange - 1.0E-6) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    private RayTraceResult trace(Player caster, Location from, Vector dir, double length,
                                 double raySize, Player ignored) {
        if (length <= 0) return null;
        return from.getWorld().rayTrace(
                from, dir, length,
                FluidCollisionMode.NEVER,
                true,
                raySize,
                entity -> entity instanceof Player p
                        && p != ignored
                        && !p.isDead()
                        && p.getGameMode() != GameMode.SPECTATOR
                        && teamManager.isEnemy(caster, p)
        );
    }

    private void spawnGrenadeTrail(Location from, Vector dir, double length) {
        World world = from.getWorld();
        for (double d = 0; d <= length; d += TRAIL_SPACING) {
            Location p = from.clone().add(dir.clone().multiply(d));
            world.spawnParticle(Particle.ITEM_SLIME, p, 6, 0.2, 0.2, 0.2, 0.05);
            world.spawnParticle(Particle.HAPPY_VILLAGER, p, 2, 0.15, 0.15, 0.15, 0);
            world.spawnParticle(Particle.SCRAPE, p, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private void spawnNeedleTrail(Location from, Vector dir, double length) {
        World world = from.getWorld();
        for (double d = 0; d <= length; d += TRAIL_SPACING) {
            Location p = from.clone().add(dir.clone().multiply(d));
            world.spawnParticle(Particle.CRIT, p, 2, 0.05, 0.05, 0.05, 0.02);
            world.spawnParticle(Particle.SCRAPE, p, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    @Override
    public String getDescription() {
        return "Fires a cactus grenade that explodes on contact or max range, splitting into "
                + needleCount + " needles in all directions.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Direct Damage", String.valueOf((int) grenadeDamage)),
                new AbilityStat("Needle Damage", String.valueOf((int) needleDamage)),
                new AbilityStat("Energy", String.valueOf((int) energyCost))
        );
    }
}