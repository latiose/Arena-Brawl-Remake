package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.joml.Math.lerp;

public class SoulCannon implements Ability {

    private final double minEnergy;
    private final double maxEnergy;
    private final double minDamage;
    private final double maxDamage;
    private final double minSpeed;
    private final double maxSpeed;
    private final double minSize;
    private final double maxSize;
    private final double maxDistance;
    private final AbilityConfig config;
    private final AbilityCost cost;
    private final EnergyManager energyManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private static final double MIN_HIT_RADIUS = 0.6;
    public SoulCannon(EnergyManager energyManager, TeamManager teamManager,
                      CombatService combatService, AbilityConfig config) {
        this.minEnergy = config.getDouble("min-energy", 10.0);
        this.maxEnergy = config.getDouble("max-energy", 100.0);
        this.minDamage = config.getDouble("min-damage", 25.0);
        this.maxDamage = config.getDouble("max-damage", 160.0);
        this.minSpeed = config.getDouble("min-speed", 0.8);
        this.maxSpeed = config.getDouble("max-speed", 2.6);
        this.minSize = config.getDouble("min-size", 0.3);
        this.maxSize = config.getDouble("max-size", 1.6);
        this.maxDistance = config.getDouble("max-target-distance", 45.0);
        this.cost = new EnergyCost(energyManager, minEnergy);
        this.energyManager = energyManager;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() { return "Soul Cannon"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Fires a soul projectile using all your energy (" + (int) minEnergy + "-" + (int) maxEnergy
                + "). The more energy spent, the bigger, faster and stronger it gets.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) minDamage + " - " + (int) maxDamage),
                new AbilityStat("Energy Cost", (int) minEnergy + " - " + (int) maxEnergy + " (all available)"),
                new AbilityStat("Speed", minSpeed * 20 + " - " + maxSpeed * 20 + " blocks/s"),
                new AbilityStat("Size", minSize + " - " + maxSize + " blocks")
        );
    }

    @Override
    public boolean activate(Player player) {
        double available = energyManager.getEnergy(player);
        if (available < minEnergy) {
            player.sendMessage("§cYou need at least " + (int) minEnergy + " energy.");
            return false;
        }

        double spent = Math.min(available, maxEnergy);
        double t = (spent - minEnergy) / (maxEnergy - minEnergy); // 0.0 -> 1.0

        final double damage = lerp(minDamage, maxDamage, t);
        final double speed = lerp(minSpeed, maxSpeed, t);
        final double size = lerp(minSize, maxSize, t);

        double extra = spent - minEnergy;
        if (extra > 0) {
            energyManager.removeEnergy(player, extra);
        }

        float pitch = (float) lerp(1.8, 0.6, t);
        MatchSoundUtils.play(config, player, Sound.ENTITY_WARDEN_SONIC_BOOM, (float) lerp(0.3, 1.0, t), pitch);
        MatchSoundUtils.play(config, player, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, pitch);

        final Location current = player.getEyeLocation().clone();
        final Vector direction = current.getDirection().normalize();
        final double hitRadius = Math.max(size, MIN_HIT_RADIUS);
        final Map<UUID, BoundingBox> previous = new HashMap<>();

        for (Player p : current.getWorld().getPlayers()) {
            previous.put(p.getUniqueId(), p.getBoundingBox());
        }

        new BukkitRunnable() {
            double traveled = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                World world = current.getWorld();
                double step = Math.min(speed, maxDistance - traveled);
                Vector start = current.toVector();

                RayTraceResult blockHit = world.rayTraceBlocks(current, direction, step, FluidCollisionMode.NEVER, true);
                double limit = blockHit != null ? start.distance(blockHit.getHitPosition()) : step;

                Player victim = null;
                Vector impactPosition = null;
                double best = limit;

                for (Player p : world.getPlayers()) {
                    BoundingBox now = p.getBoundingBox();
                    BoundingBox before = previous.getOrDefault(p.getUniqueId(), now);
                    previous.put(p.getUniqueId(), now.clone());

                    if (p.equals(player) || p.isDead() || p.getGameMode() == GameMode.SPECTATOR) continue;
                    if (!teamManager.isEnemy(player, p)) continue;

                    BoundingBox swept = now.clone().union(before).expand(hitRadius);
                    RayTraceResult result = swept.rayTrace(start, direction, limit);
                    if (result == null) continue;

                    double distance = start.distance(result.getHitPosition());
                    if (distance <= best) {
                        best = distance;
                        victim = p;
                        impactPosition = result.getHitPosition();
                    }
                }

                if (victim != null) {
                    cancel();
                    Location impact = impactPosition.toLocation(world);
                    drawProjectile(current, direction, best, size, t);
                    combatService.applyAbilityDamage(player, victim, damage, getName(), impact);
                    impactEffect(impact, size, t);
                    return;
                }

                if (blockHit != null) {
                    cancel();
                    Location impact = blockHit.getHitPosition().toLocation(world);
                    drawProjectile(current, direction, limit, size, t);
                    impactEffect(impact, size, t);
                    return;
                }

                drawProjectile(current, direction, step, size, t);
                current.add(direction.clone().multiply(step));
                traveled += step;

                if (traveled >= maxDistance) {
                    cancel();
                    impactEffect(current.clone(), size, t);
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        return true;
    }

    private void drawProjectile(Location from, Vector direction, double length, double size, double t) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int spheresPerTick = Math.max(1, (int) Math.ceil(length / Math.max(0.5, size)));
        int pointsPerSphere = (int) lerp(8, 30, t);

        for (int s = 0; s < spheresPerTick; s++) {
            double d = spheresPerTick == 1 ? length : length * s / (spheresPerTick - 1);
            Location center = from.clone().add(direction.clone().multiply(d));

            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 2, size * 0.2, size * 0.2, size * 0.2, 0.01);

            for (int i = 0; i < pointsPerSphere; i++) {
                double u = rnd.nextDouble() * 2 - 1;
                double phi = rnd.nextDouble() * 2 * Math.PI;
                double r = Math.sqrt(1 - u * u);
                Location p = center.clone().add(size * r * Math.cos(phi), size * u, size * r * Math.sin(phi));
                p.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(70, 220, 255), (float) Math.max(0.8, size * 1.2)));
            }
        }
        from.getWorld().spawnParticle(Particle.SOUL, from, 1 + (int) (t * 4), size * 0.3, size * 0.3, size * 0.3, 0.02);
    }

    private void impactEffect(Location impact, double size, double t) {
        impact.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, impact, (int) lerp(15, 80, t), size * 0.6, size * 0.6, size * 0.6, 0.08);
        impact.getWorld().spawnParticle(Particle.SOUL, impact, (int) lerp(10, 50, t), size * 0.6, size * 0.6, size * 0.6, 0.1);
        impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 1 + (int) (t * 3), size * 0.3, size * 0.3, size * 0.3, 0.0);

        impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, (float) lerp(0.4, 1.2, t), (float) lerp(1.8, 0.8, t));
        impact.getWorld().playSound(impact, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 0.7f);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}