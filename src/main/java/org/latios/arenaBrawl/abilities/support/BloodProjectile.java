package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

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

        Snowball orb = player.launchProjectile(Snowball.class);
        orb.setGravity(false);

        Vector direction = target.toVector().subtract(orb.getLocation().toVector());
        if (direction.lengthSquared() > 1.0E-6) {
            orb.setVelocity(direction.normalize().multiply(projectileSpeed));
        }

        MatchSoundUtils.play(config, player, Sound.ENTITY_WITCH_THROW, 0.9f, 0.8f);

        new BloodOrbTask(orb, player, target).runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    /** Picks a random spot on the ground a fair distance away from the caster. */
    private Location randomGroundLocation(Player player) {
        Location origin = player.getLocation();
        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2.0);
        double distance = ThreadLocalRandom.current().nextDouble(minDistance, maxDistance);

        double x = origin.getX() + Math.cos(angle) * distance;
        double z = origin.getZ() + Math.sin(angle) * distance;
        double y = origin.getWorld().getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;

        return new Location(origin.getWorld(), x, y, z);
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

        private final Snowball orb;
        private final Player owner;
        private final Location target;
        private Location lastLocation;
        private int flightTicks = 0;
        private boolean landed = false;
        private long landedAt = 0L;

        private BloodOrbTask(Snowball orb, Player owner, Location target) {
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
                if (orb.isValid() && !orb.isDead()) {
                    lastLocation = orb.getLocation().clone();
                    Vector step = target.toVector().subtract(lastLocation.toVector());
                    if (step.lengthSquared() <= 1.0) {
                        land(target.clone());
                        return;
                    }
                    orb.setVelocity(step.normalize().multiply(projectileSpeed));
                } else {
                    land(lastLocation.clone());
                    return;
                }

                flightTicks++;
                if (flightTicks >= maxFlightTicks) {
                    land(lastLocation.clone());
                }
                return;
            }

            long elapsed = System.currentTimeMillis() - landedAt;
            if (elapsed >= orbLifetimeMs) {
                fade();
                cancel();
                return;
            }

            lastLocation.getWorld().spawnParticle(Particle.DUST, lastLocation, 6, 0.25, 0.25, 0.25,
                    new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f));
            lastLocation.getWorld().spawnParticle(Particle.CRIT, lastLocation, 2, 0.15, 0.15, 0.15, 0.0);

            if (owner.getWorld().equals(lastLocation.getWorld())
                    && owner.getLocation().distanceSquared(lastLocation) <= collectRadius * collectRadius) {
                collect();
                cancel();
            }
        }

        private void land(Location at) {
            removeOrb();
            lastLocation = at;
            landed = true;
            landedAt = System.currentTimeMillis();
            lastLocation.getWorld().playSound(lastLocation, Sound.BLOCK_SLIME_BLOCK_BREAK, 0.9f, 0.7f);
            lastLocation.getWorld().spawnParticle(Particle.DUST, lastLocation, 20, 0.3, 0.2, 0.3,
                    new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.2f));
        }

        private void collect() {
            healthManager.heal(owner, healAmount, getName());

            lastLocation.getWorld().spawnParticle(Particle.HEART, lastLocation.clone().add(0, 1, 0), 18, 0.4, 0.5, 0.4, 0.02);
            lastLocation.getWorld().spawnParticle(Particle.DUST, lastLocation, 30, 0.4, 0.4, 0.4,
                    new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.4f));

            MatchSoundUtils.play(config, owner, Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.1f);
            MatchSoundUtils.play(config, owner, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.6f);
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
