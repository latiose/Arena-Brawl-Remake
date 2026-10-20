package org.latios.arenaBrawl.abilities.support;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BloodProjectile implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final double healAmount;
    private final double projectileSpeed;
    private final double minDistance;
    private final double maxDistance;
    private final int maxFlightTicks;
    private final long orbLifetimeMs;
    private final double collectRadius;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public BloodProjectile(Plugin plugin, CooldownManager cooldownManager, PlayerHealthManager healthManager,
                           CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.healAmount = config.getDouble("heal-amount", 450.0);
        this.projectileSpeed = config.getDouble("projectile-speed", 0.6);
        this.minDistance = config.getDouble("min-distance", 4.0);
        this.maxDistance = Math.max(minDistance + 0.5, config.getDouble("max-distance", 9.0));
        this.maxFlightTicks = config.getInt("max-flight-ticks", 60);
        this.orbLifetimeMs = config.getLong("orb-lifetime-ms", 6000L);
        this.collectRadius = config.getDouble("collect-radius", 2.0);

        this.cost = new CooldownCost(cooldownManager, "bloodprojectile", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Blood Projectile"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Location target = randomGroundLocation(player);
        Location start = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.4));

        // A display entity (not a physics projectile) so decorative partial blocks like
        // stairs and slabs can never trap the orb mid-flight.
        ItemDisplay orb = start.getWorld().spawn(start, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.REDSTONE));
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setPersistent(false);
            display.setTeleportDuration(1);
            display.setInterpolationDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });
        EntityCleanupUtils.markAsArenaEntity(orb);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_THROW, 10.0f, 0.8f);

        new BloodOrbTask(orb, player, target).runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    private Location randomGroundLocation(Player player) {
        World world = player.getWorld();
        Location origin = player.getLocation();
        int originY = origin.getBlockY();

        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2.0);
            double distance = ThreadLocalRandom.current().nextDouble(minDistance, maxDistance);
            int bx = (int) Math.floor(origin.getX() + Math.cos(angle) * distance);
            int bz = (int) Math.floor(origin.getZ() + Math.sin(angle) * distance);

            if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
                continue;
            }

            Location candidate = findFloorNearY(world, bx, bz, originY);
            if (candidate == null) {
                continue;
            }

            if (!hasClearPath(origin, candidate)) {
                continue;
            }

            return candidate;
        }

        return origin.clone();
    }


    private Location findFloorNearY(World world, int bx, int bz, int originY) {
        int maxUp = 1;
        int maxDown = 3;

        for (int y = originY + maxUp; y >= originY - maxDown; y--) {
            Block floor = world.getBlockAt(bx, y, bz);
            if (!isSolidFloor(floor)) {
                continue;
            }
            if (!world.getBlockAt(bx, y + 1, bz).isPassable()
                    || !world.getBlockAt(bx, y + 2, bz).isPassable()) {
                continue;
            }
            return new Location(world, bx + 0.5, y + 1.0, bz + 0.5);
        }
        return null;
    }

    private static boolean isSolidFloor(Block block) {
        if (!block.isSolid()) {
            return false;
        }
        Collection<BoundingBox> boxes = block.getCollisionShape().getBoundingBoxes();
        if (boxes.size() != 1) {
            return false;
        }
        BoundingBox box = boxes.iterator().next();
        return box.getWidthX() >= 0.9 && box.getWidthZ() >= 0.9 && box.getHeight() >= 0.9;
    }

    private boolean hasClearPath(Location from, Location to) {
        Location start = from.clone().add(0, 1.0, 0);
        Location end = to.clone().add(0, 0.5, 0);
        Vector dir = end.toVector().subtract(start.toVector());
        double len = dir.length();
        if (len < 0.01) {
            return true;
        }
        return from.getWorld().rayTraceBlocks(start, dir.normalize(), len,
                FluidCollisionMode.NEVER, true) == null;
    }

    @Override
    public String getDescription() {
        return "Hurls a blood orb to a random spot near you. Sprint over to reclaim it and heal yourself for a large amount.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", String.valueOf((int) healAmount)),
                new AbilityStat("Throw Distance", (int) minDistance + " - " + (int) maxDistance + " blocks"),
                new AbilityStat("Collect Radius", (int) collectRadius + " blocks"),
                new AbilityStat("Orb Lifetime", (orbLifetimeMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    private final class BloodOrbTask extends BukkitRunnable {

        private final ItemDisplay orb;
        private final Player owner;
        private final Location target;
        private Location lastLocation;
        private int flightTicks = 0;
        private boolean landed = false;
        private long landedAt = 0L;
        private int landedTicks = 0;

        private BloodOrbTask(ItemDisplay orb, Player owner, Location target) {
            this.orb = orb;
            this.owner = owner;
            this.target = target;
            this.lastLocation = orb.getLocation().clone();
        }

        @Override
        public void run() {
            if (!owner.isOnline()) {
                removeOrb();
                cancel();
                return;
            }

            if (!landed) {
                Location current = orb.isValid() ? orb.getLocation().clone() : lastLocation.clone();
                Vector step = target.toVector().subtract(current.toVector());
                double distance = step.length();

                if (distance <= Math.max(0.05, projectileSpeed)) {
                    land(target.clone());
                    return;
                }

                // Move by teleport so block collisions (stairs, slabs, fences...) can't stall the orb.
                Vector move = step.multiply(1.0 / distance).multiply(projectileSpeed);
                Location next = current.clone().add(move);

                spawnTrail(current, next);
                lastLocation = next;
                if (orb.isValid()) {
                    orb.teleport(next);
                    orb.setRotation(flightTicks * 36f, 0f);
                }

                flightTicks++;
                if (flightTicks >= maxFlightTicks) {
                    land(next.clone());
                }
                return;
            }

            long elapsed = System.currentTimeMillis() - landedAt;
            if (elapsed >= orbLifetimeMs) {
                fade();
                cancel();
                return;
            }

            landedTicks++;
            spawnLandingMarker(lastLocation);
            if (landedTicks % 5 == 0) {
                spawnLandingRing(lastLocation);
            }

            if (owner.getWorld().equals(lastLocation.getWorld())
                    && owner.getLocation().distanceSquared(lastLocation) <= collectRadius * collectRadius) {
                collect();
                cancel();
            }
        }

        /** Draws a red dust trail so the orb's flight path is easy to follow. */
        private void spawnTrail(Location from, Location to) {
            Vector delta = to.toVector().subtract(from.toVector());
            double length = delta.length();
            int points = Math.max(1, (int) Math.ceil(length / 0.2));
            for (int i = 0; i <= points; i++) {
                Location point = from.clone().add(delta.clone().multiply((double) i / points));
                point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.1f));
            }
            to.getWorld().spawnParticle(Particle.CRIMSON_SPORE, to, 2, 0.1, 0.1, 0.1, 0.0);
        }

        /** Vertical beacon plus a glowing core marking where the orb landed. */
        private void spawnLandingMarker(Location center) {
            for (int i = 0; i < 7; i++) {
                Location point = center.clone().add(0, 0.2 + i * 0.35, 0);
                point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(210, 0, 0), 1.0f));
            }
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2,
                    new Particle.DustOptions(Color.fromRGB(255, 30, 30), 1.5f));
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 1.6, 0), 1, 0.05, 0.15, 0.05, 0.0);
        }

        /** Ground ring so the pickup spot stands out on the floor. */
        private void spawnLandingRing(Location center) {
            int segments = 16;
            double radius = 0.9;
            for (int i = 0; i < segments; i++) {
                double angle = (i / (double) segments) * Math.PI * 2.0;
                Location point = center.clone().add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);
                point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(230, 20, 20), 1.2f));
            }
        }

        private void land(Location at) {
            removeOrb();
            lastLocation = at;
            landed = true;
            landedAt = System.currentTimeMillis();
            landedTicks = 0;
            lastLocation.getWorld().playSound(lastLocation, Sound.BLOCK_SLIME_BLOCK_BREAK, 0.9f, 0.7f);
            lastLocation.getWorld().spawnParticle(Particle.DUST, lastLocation, 40, 0.4, 0.3, 0.4,
                    new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.4f));
            lastLocation.getWorld().spawnParticle(Particle.END_ROD, lastLocation.clone().add(0, 0.5, 0),
                    12, 0.3, 0.4, 0.3, 0.02);
            spawnLandingRing(lastLocation);
        }

        private void collect() {
            healthManager.heal(owner, healAmount, getName());

            lastLocation.getWorld().spawnParticle(Particle.HEART, lastLocation.clone().add(0, 1, 0), 18, 0.4, 0.5, 0.4, 0.02);
            lastLocation.getWorld().spawnParticle(Particle.DUST, lastLocation, 30, 0.4, 0.4, 0.4,
                    new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.4f));

            owner.getWorld().playSound(owner.getLocation(), Sound.ENTITY_GENERIC_DRINK, 10.0f, 1.1f);
            owner.getWorld().playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 10.0f, 1.6f);
        }

        private void fade() {
            lastLocation.getWorld().spawnParticle(Particle.DUST, lastLocation, 10, 0.3, 0.3, 0.3,
                    new Particle.DustOptions(Color.fromRGB(90, 0, 0), 1.0f));
            lastLocation.getWorld().playSound(lastLocation, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.4f);
        }

        private void removeOrb() {
            if (orb.isValid()) orb.remove();
        }
    }
}
