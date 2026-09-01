package org.latios.arenaBrawl.abilities.utility;

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
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class RocketGrab implements Ability {

    private final long cooldownMs;
    private final double maxRange;
    private final double step;
    private final int pullTicks;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;

    public RocketGrab(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                      CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.maxRange = config.getDouble("max-range", 20.0);
        this.step = config.getDouble("step", 1.5);
        this.pullTicks = config.getInt("pull-ticks", 20);

        this.cost = new CooldownCost(cooldownManager, "rocketgrab", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
    }

    @Override
    public String getName() {
        return "Rocket Grab";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = startLoc.getDirection().normalize();

        player.getWorld().playSound(startLoc, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 0.6f);

        new BukkitRunnable() {
            private Location currentLoc = startLoc.clone();
            private double distanceTraveled = 0;

            @Override
            public void run() {
                currentLoc.add(direction.clone().multiply(step));
                distanceTraveled += step;

                currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 2, 0.05, 0.05, 0.05, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.SCRAPE, currentLoc, 1, 0, 0, 0, 0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_ANVIL_PLACE, 0.6f, 1.8f);
                    cancel();
                    return;
                }

                for (Player enemy : currentLoc.getWorld().getPlayers()) {
                    if (!teamManager.isEnemy(player, enemy) || enemy.isDead()) continue;

                    if (enemy.getBoundingBox().expand(0.3, 0.3, 0.3).contains(currentLoc.getX(), currentLoc.getY(), currentLoc.getZ())) {
                        pullEnemyOverTime(player, enemy);
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

    private void pullEnemyOverTime(Player caster, Player target) {
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.8f);

        new BukkitRunnable() {
            private int elapsedTicks = 0;

            @Override
            public void run() {
                if (elapsedTicks >= pullTicks || target.isDead() || !target.isOnline()) {
                    cancel();
                    return;
                }

                Location targetLoc = target.getLocation();
                Location destination = caster.getLocation().add(caster.getLocation().getDirection().setY(0).normalize().multiply(1.2));

                Vector pullVector = destination.toVector().subtract(targetLoc.toVector());

                int remainingTicks = pullTicks - elapsedTicks;
                Vector velocity = pullVector.divide(new Vector(remainingTicks, remainingTicks, remainingTicks));

                target.setVelocity(velocity);

                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0), 2, 0.1, 0.1, 0.1, 0.02);

                elapsedTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public String getDescription() {
        return "Fires a mechanical claw that grabs the first enemy hit and pulls them to your location over 1 second.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}