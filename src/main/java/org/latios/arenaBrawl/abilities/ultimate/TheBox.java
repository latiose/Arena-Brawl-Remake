package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

public class TheBox implements Ability {

    private final double radius;
    private final double damage;
    private final long slowDurationMs;
    private final long durationTicks;
    private final long chargeTimeMillis;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final Plugin plugin;
    private final CooldownManager cooldownManager;

    public TheBox(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                  CombatService combatService, DebuffManager debuffManager, UsageManager usageManager,
                  AbilityConfig config) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.cooldownManager = cooldownManager;

        this.radius = config.getDouble("radius", 4.0);
        this.damage = config.getDouble("damage", 350.0);
        this.slowDurationMs = config.getLong("slow-duration-ms", 4000L);
        this.durationTicks = config.getLong("duration-ticks", 100L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);

        this.cost = new UltimateCost(cooldownManager, usageManager, "thebox");
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "thebox", chargeTimeMillis);
    }

    @Override
    public String getName() { return "The Box"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation();
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.5f);

        List<Location> vertices = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            vertices.add(center.clone().add(x, 0, z));
        }

        List<WallSegment> walls = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Location start = vertices.get(i);
            Location end = vertices.get((i + 1) % 5);
            walls.add(new WallSegment(start, end));
        }

        new BukkitRunnable() {
            int ticksElapsed = 0;
            boolean triggered = false;

            @Override
            public void run() {
                ticksElapsed += 2;

                if (ticksElapsed >= durationTicks || triggered || !player.isOnline()) {
                    cancel();
                    return;
                }

                for (WallSegment wall : walls) {
                    wall.renderParticles();
                }

                for (Player enemy : center.getWorld().getPlayers()) {
                    if (!teamManager.isEnemy(player, enemy) || enemy.isDead()) continue;

                    for (WallSegment wall : walls) {
                        if (wall.isNear(enemy.getLocation())) {
                            triggered = true;

                            combatService.applyAbilityDamage(player, enemy, damage, getName());
                            debuffManager.tryApply(enemy, DebuffType.SLOW, slowDurationMs);

                            enemy.getWorld().playSound(enemy.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 0.6f);
                            enemy.getWorld().spawnParticle(Particle.SOUL, enemy.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.1);

                            cancel();
                            return;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private static class WallSegment {
        private final Location start;
        private final Location end;

        public WallSegment(Location start, Location end) {
            this.start = start;
            this.end = end;
        }

        public void renderParticles() {
            Vector dir = end.toVector().subtract(start.toVector());
            double length = dir.length();
            dir.normalize();

            for (double d = 0; d <= length; d += 0.4) {
                Location point = start.clone().add(dir.clone().multiply(d));
                for (double y = 0; y <= 2.0; y += 0.5) {
                    point.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point.clone().add(0, y, 0), 1, 0, 0, 0, 0);
                }
            }
        }

        public boolean isNear(Location loc) {
            Vector startVec = start.toVector();
            Vector endVec = end.toVector();
            Vector pointVec = loc.toVector();

            Vector line = endVec.clone().subtract(startVec);
            double lenSq = line.lengthSquared();
            if (lenSq == 0) return startVec.distance(pointVec) < 0.8;

            double t = Math.max(0, Math.min(1, pointVec.clone().subtract(startVec).dot(line) / lenSq));
            Vector projection = startVec.clone().add(line.multiply(t));

            return projection.distance(pointVec) <= 0.8 && Math.abs(loc.getY() - start.getY()) <= 2.2;
        }
    }

    @Override
    public String getDescription() {
        return "Erects a 5-wall spectral prison. Enemy touching a wall suffers heavy damage and slow.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Slow Duration", (slowDurationMs / 1000) + "s"),
                new AbilityStat("Wall Lifetime", (durationTicks / 20) + "s")
        );
    }
}