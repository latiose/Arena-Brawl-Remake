package org.latios.arenaBrawl.abilities.utility;

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
import org.latios.arenaBrawl.abilities.structures.ProjectileInterceptionService;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class RocketGrab implements Ability {

    private final long cooldownMs;
    private final double maxRange;
    private final double step;
    private final int pullTicks;
    private final double maxPullSpeed;
    private final AbilityConfig config;
    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;

    public RocketGrab(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                      CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.maxRange = config.getDouble("max-range", 24.0);
        this.step = config.getDouble("step", 1.0);
        this.pullTicks = config.getInt("pull-ticks", 20);
        this.maxPullSpeed = config.getDouble("max-pull-speed", 3.0);

        this.cost = new CooldownCost(cooldownManager, "rocketgrab", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Grappling Hook";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = startLoc.getDirection().normalize();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 10.0f, 0.6f);

        new BukkitRunnable() {
            private Location currentLoc = startLoc.clone();
            private double distanceTraveled = 0;

            @Override
            public void run() {
                currentLoc.add(direction.clone().multiply(step));
                distanceTraveled += step;

                currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.SCRAPE, currentLoc, 1, 0, 0, 0, 0);

                if (currentLoc.getBlock().getType().isSolid()
                        || ProjectileInterceptionService.isBlocked(player, currentLoc)) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 10.0f, 1.8f);
                    cancel();
                    Location destination = currentLoc.clone().subtract(direction.clone().multiply(1.5));
                    pullCaster(player, destination);
                    return;
                }

                for (Player enemy : currentLoc.getWorld().getPlayers()) {
                    if (!teamManager.isEnemy(player, enemy) || enemy.isDead()
                            || enemy.getGameMode() == GameMode.SPECTATOR) continue;

                    if (enemy.getBoundingBox().expand(1.5, 1.5, 1.5).contains(currentLoc.getX(), currentLoc.getY(), currentLoc.getZ())) {
                        pullEnemyToCaster(player, enemy);
                        cancel();
                        return;
                    }
                }

                if (distanceTraveled >= maxRange) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void pullCaster(Player caster, Location destination) {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CHAIN_BREAK, 10.0f, 0.8f);

        new BukkitRunnable() {
            private int elapsedTicks = 0;

            @Override
            public void run() {
                if (elapsedTicks >= pullTicks || !caster.isOnline() || caster.isDead()) {
                    cancel();
                    return;
                }

                Location origin = caster.getLocation();
                Vector delta = destination.toVector().subtract(origin.toVector());
                if (delta.lengthSquared() < 1.0) {
                    cancel();
                    return;
                }

                int remainingTicks = Math.max(1, pullTicks - elapsedTicks);
                Vector velocity = delta.multiply(1.0 / remainingTicks);
                if (velocity.length() > maxPullSpeed) {
                    velocity.normalize().multiply(maxPullSpeed);
                }

                caster.setVelocity(velocity);
                drawRope(origin.add(0, 1.0, 0), destination.clone().add(0, 1.0, 0));

                elapsedTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void pullEnemyToCaster(Player caster, Player target) {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CHAIN_BREAK, 10.0f, 0.8f);

        new BukkitRunnable() {
            private int elapsedTicks = 0;

            @Override
            public void run() {
                if (elapsedTicks >= pullTicks || !target.isOnline() || target.isDead()
                        || !caster.isOnline() || caster.isDead()) {
                    cancel();
                    return;
                }

                Location destination = caster.getLocation().add(
                        caster.getLocation().getDirection().setY(0).normalize().multiply(1.2));
                Vector delta = destination.toVector().subtract(target.getLocation().toVector());

                int remainingTicks = Math.max(1, pullTicks - elapsedTicks);
                Vector velocity = delta.multiply(1.0 / remainingTicks);
                if (velocity.length() > maxPullSpeed) {
                    velocity.normalize().multiply(maxPullSpeed);
                }
                velocity.setY(Math.max(velocity.getY(), 0.15));

                target.setVelocity(velocity);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0), 2, 0.1, 0.1, 0.1, 0.02);
                drawRope(target.getLocation().add(0, 1.0, 0), caster.getLocation().add(0, 1.0, 0));

                elapsedTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawRope(Location from, Location to) {
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        int steps = Math.max(2, (int) (length / 0.4));

        for (int i = 0; i <= steps; i++) {
            Location point = from.clone().add(delta.clone().multiply((double) i / steps));
            point.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public String getDescription() {
        return "Fires a grappling hook. Hitting terrain pulls you toward it; hitting an enemy pulls them to you without dealing damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
