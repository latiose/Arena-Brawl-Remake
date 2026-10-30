package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spinning melee attack. The caster spins in place for a short window, hitting
 * every enemy around them several times while moving, and gains a short burst
 * of speed so they can stay glued to their target.
 *
 * <p>Speed note: the game always runs with Speed I (amplifier 0) as its base,
 * so a speed bonus only makes sense with an amplifier of 1 or higher. The
 * speed buff is skipped entirely when {@code speed-amplifier} is 0.
 */
public class Whirlwind implements Ability {

    private static final String SPEED_SOURCE = "Whirlwind";

    private final double energyCost;
    private final double damage;
    private final double radius;
    private final double heightTolerance;
    private final int durationTicks;
    private final int hitIntervalTicks;
    private final int speedAmplifier;
    private final double knockback;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final SpeedBuffManager speedBuffManager;
    private final Set<UUID> spinning = ConcurrentHashMap.newKeySet();

    public Whirlwind(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                     CombatService combatService, SpeedBuffManager speedBuffManager,
                     AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 65.0);
        this.damage = config.getDouble("damage", 32.0);
        this.radius = config.getDouble("radius", 3.5);
        this.heightTolerance = config.getDouble("height-tolerance", 2.0);
        this.durationTicks = config.getInt("duration-ticks", 20);
        this.hitIntervalTicks = Math.max(1, config.getInt("hit-interval-ticks", 5));
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.knockback = config.getDouble("knockback", 0.0);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.speedBuffManager = speedBuffManager;
    }

    @Override
    public String getName() {
        return "Whirlwind";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        UUID id = player.getUniqueId();
        // The offensive slot auto-repeats while right-click is held, so ignore
        // re-casts made while a spin is already running.
        if (!spinning.add(id)) return false;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.5f, 1.4f);

        // Amplifier 0 would be the game's base Speed I, so skip the buff entirely.
        if (speedAmplifier > 0) {
            speedBuffManager.applyBuff(player, speedAmplifier, durationTicks * 50L, SPEED_SOURCE);
        }

        new BukkitRunnable() {
            private int ticks = 0;
            private double angle = 0.0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR) {
                    stop();
                    cancel();
                    return;
                }

                ticks++;
                angle += 0.55;
                spawnSpinEffects(player, angle);

                if (ticks % hitIntervalTicks == 0) {
                    hitNearby(player);
                }

                if (ticks >= durationTicks) {
                    stop();
                    cancel();
                }
            }

            private void stop() {
                spinning.remove(id);
                if (speedAmplifier > 0) {
                    speedBuffManager.removeBuff(player, SPEED_SOURCE);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void hitNearby(Player player) {
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) return;

        double radiusSq = radius * radius;

        for (Entity nearby : world.getNearbyEntities(center, radius, heightTolerance, radius)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.equals(player) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, target)) continue;

            Location targetLocation = target.getLocation();
            double dx = targetLocation.getX() - center.getX();
            double dz = targetLocation.getZ() - center.getZ();
            if (dx * dx + dz * dz > radiusSq) continue;
            if (Math.abs(targetLocation.getY() - center.getY()) > heightTolerance) continue;

            combatService.applyAbilityDamage(
                    player, target, damage, getName(),
                    targetLocation.clone().add(0, 1.0, 0)
            );

            if (knockback > 0.0) {
                Vector push = new Vector(dx, 0, dz);
                if (push.lengthSquared() > 0.0001) {
                    push.normalize().multiply(knockback);
                    Vector velocity = target.getVelocity();
                    target.setVelocity(new Vector(
                            velocity.getX() + push.getX(),
                            velocity.getY(),
                            velocity.getZ() + push.getZ()
                    ));
                }
            }

            world.playSound(targetLocation, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.3f);
            world.spawnParticle(
                    Particle.CRIT, targetLocation.clone().add(0, 1.0, 0),
                    8, 0.25, 0.35, 0.25, 0.1
            );
        }
    }

    private void spawnSpinEffects(Player player, double angle) {
        World world = player.getWorld();
        Location center = player.getLocation();

        world.spawnParticle(
                Particle.SWEEP_ATTACK, center.clone().add(0, 1.0, 0),
                1, 0.2, 0.0, 0.2, 0.0
        );

        for (int i = 0; i < 4; i++) {
            double a = angle + (Math.PI / 2.0) * i;
            Location point = center.clone().add(
                    Math.cos(a) * radius * 0.75,
                    1.0,
                    Math.sin(a) * radius * 0.75
            );
            world.spawnParticle(Particle.END_ROD, point, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public String getDescription() {
        return "Spin in place for a short window, striking every nearby enemy several times "
                + "as you move. Also grants a brief burst of speed so you can stay on your target.";
    }

    @Override
    public List<AbilityStat> getStats() {
        int hits = Math.max(1, durationTicks / hitIntervalTicks);
        String speed = speedAmplifier > 0
                ? "Speed " + (speedAmplifier + 1) + " (" + (durationTicks / 20.0) + "s)"
                : "None";

        return List.of(
                new AbilityStat("Damage", (int) damage + " HP x" + hits),
                new AbilityStat("Total", (int) (damage * hits) + " HP"),
                new AbilityStat("Radius", radius + " blocks"),
                new AbilityStat("Speed", speed),
                new AbilityStat("Energy", (int) energyCost + "")
        );
    }
}
