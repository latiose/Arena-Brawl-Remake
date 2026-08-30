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
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class RocketGrab implements Ability {

    private static final long COOLDOWN_MS = 30_000;
    private static final double MAX_RANGE = 20.0;
    private static final double STEP = 1.5;
    private static final int PULL_TICKS = 20;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;

    public RocketGrab(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager, CombatUpgradeManager combatUpgradeManager) {
        this.plugin = plugin;
        this.cost = new CooldownCost(cooldownManager, "rocketgrab", COOLDOWN_MS,combatUpgradeManager);
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
                currentLoc.add(direction.clone().multiply(STEP));
                distanceTraveled += STEP;

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

                if (distanceTraveled >= MAX_RANGE) {
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
                if (elapsedTicks >= PULL_TICKS || target.isDead() || !target.isOnline()) {
                    cancel();
                    return;
                }

                Location targetLoc = target.getLocation();
                Location destination = caster.getLocation().add(caster.getLocation().getDirection().setY(0).normalize().multiply(1.2));

                Vector pullVector = destination.toVector().subtract(targetLoc.toVector());

                int remainingTicks = PULL_TICKS - elapsedTicks;
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
                new AbilityStat("Range", (int) MAX_RANGE + "m"),
                new AbilityStat("Cooldown", (COOLDOWN_MS / 1000) + "s")
        );
    }
}