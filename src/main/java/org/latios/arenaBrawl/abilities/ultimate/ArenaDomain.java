package org.latios.arenaBrawl.abilities.ultimate;

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

import org.latios.arenaBrawl.abilities.cost.UltimateCost;

import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ArenaDomain implements Ability {

    private static final long CHARGE_TIME_MILLIS = 60_000;
    private static final double RADIUS = 8.0;
    private static final int DURATION_SECONDS = 8;
    private static final double HEIGHT = 5.0;

    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final AbilityCost cost;
    private final TeamManager teamManager;

    public ArenaDomain(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "arenadomain");
        this.teamManager = teamManager;
    }

    @Override
    public String getName() {
        return "Arena Domain";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "arenadomain", CHARGE_TIME_MILLIS);
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation().clone();
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.8f);
        new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> trappedEnemies = new HashSet<>();
            final double radiusSquared = RADIUS * RADIUS;

            @Override
            public void run() {
                ticks++;

                if (ticks >= DURATION_SECONDS * 20 || !player.isOnline() || player.isDead()) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 1.2f);
                    cancel();
                    return;
                }

                Location playerLoc = player.getLocation();
                Vector fromCenterToPlayer = playerLoc.toVector().subtract(center.toVector()).setY(0);
                if (fromCenterToPlayer.lengthSquared() > radiusSquared) {
                    center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                    cancel();
                    return;
                }

                drawCylinder(center);

                for (Player target : center.getWorld().getPlayers()) {
                    if (target.getGameMode() == GameMode.SPECTATOR || target.isDead()) continue;
                    if (!teamManager.isEnemy(player, target)) continue;

                    Location targetLoc = target.getLocation();
                    Vector fromCenterToTarget = targetLoc.toVector().subtract(center.toVector());
                    fromCenterToTarget.setY(0); // Ignore Y for horizontal radius check

                    double distanceSquared = fromCenterToTarget.lengthSquared();

                    if (distanceSquared <= radiusSquared) {
                        trappedEnemies.add(target.getUniqueId());
                    }

                    if (trappedEnemies.contains(target.getUniqueId()) && distanceSquared > radiusSquared) {
                        fromCenterToTarget.normalize().multiply(RADIUS - 0.2);

                        Location newLoc = center.clone().add(fromCenterToTarget);
                        newLoc.setY(targetLoc.getY());
                        newLoc.setYaw(targetLoc.getYaw());
                        newLoc.setPitch(targetLoc.getPitch());

                        target.teleport(newLoc);
                        target.playSound(targetLoc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        target.getWorld().spawnParticle(Particle.CRIT, targetLoc.add(0, 1, 0), 10, 0.2, 0.5, 0.2, 0.05);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void drawCylinder(Location center) {
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
            double x = Math.cos(angle) * RADIUS;
            double z = Math.sin(angle) * RADIUS;

            Location pointGround = center.clone().add(x, 0, z);
            Location pointMid = center.clone().add(x, HEIGHT / 2, z);
            Location pointTop = center.clone().add(x, HEIGHT, z);

            center.getWorld().spawnParticle(Particle.FLAME, pointGround, 1, 0, 0, 0, 0);

            if (Math.random() > 0.7) {
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pointMid, 1, 0, 0, 0, 0);
                center.getWorld().spawnParticle(Particle.FLAME, pointTop, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Deploys an inescapable arena. Enemies can enter but cannot leave. If you step out, the arena breaks.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Radius", RADIUS + "m"),
                new AbilityStat("Duration", DURATION_SECONDS + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}