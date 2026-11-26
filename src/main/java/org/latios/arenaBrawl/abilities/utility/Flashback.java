package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Flashback implements Ability {

    private record PositionSnapshot(long time, Location location) {}

    private static final long SAMPLE_INTERVAL_TICKS = 5L;
    private static final long MAX_HISTORY_MILLIS = 60_000L;
    private static final Map<UUID, Deque<PositionSnapshot>> HISTORY = new HashMap<>();
    private static boolean trackingStarted;

    private final long historyMillis;
    private final double radius;
    private final long immobilizeDurationMs;
    private final long cooldownMs;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    public Flashback(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                     DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager,
                     AbilityConfig config) {
        this.historyMillis = config.getInt("history-seconds", 6) * 1000L;
        this.radius = config.getDouble("radius", 4.0);
        this.immobilizeDurationMs = config.getLong("immobilize-duration-ms", 1500L);
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);

        this.cost = new CooldownCost(cooldownManager, "flashback", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;

        ensureTracking(plugin);
    }

    @Override
    public String getName() {
        return "Flashback";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        HISTORY.remove(player.getUniqueId());
    }

    @Override
    public boolean activate(Player player) {
        Location origin = player.getLocation().clone();
        Location destination = safeDestination(getPastLocation(player), player);
        if(player.getGameMode()!=GameMode.SPECTATOR && !player.isDead() && player.getWorld().equals(destination.getWorld())) {
            player.teleport(destination);
            player.setFallDistance(0);

            drawArrival(origin, destination);
            player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);
            player.getWorld().playSound(destination, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.8f);

            immobilizeNearby(player, destination);
        }
        return true;
    }

    private Location getPastLocation(Player player) {
        Deque<PositionSnapshot> history = HISTORY.get(player.getUniqueId());
        if (history == null || history.isEmpty()) {
            return player.getLocation().clone();
        }

        long targetTime = System.currentTimeMillis() - historyMillis;
        Location result = history.peekFirst().location();

        for (PositionSnapshot snapshot : history) {
            if (snapshot.time() > targetTime) break;
            result = snapshot.location();
        }

        return result.clone();
    }

    private Location safeDestination(Location base, Player player) {
        Location destination = base.clone();

        if (destination.getWorld() == null || !destination.getWorld().equals(player.getWorld())) {
            destination = player.getLocation().clone();
        }

        for (int i = 0; i < 4 && isBlocked(destination); i++) {
            destination.add(0, 1, 0);
        }

        destination.setDirection(player.getLocation().getDirection());
        return destination;
    }

    private boolean isBlocked(Location location) {
        return location.getBlock().getType().isSolid()
                || location.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private void immobilizeNearby(Player caster, Location center) {
        double radiusSquared = radius * radius;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(caster) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (target.getLocation().distanceSquared(center) > radiusSquared) continue;

            debuffManager.tryApply(caster, target, DebuffType.IMMOBILIZE, immobilizeDurationMs);

            target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1.0, 0),
                    20, 0.3, 0.5, 0.3, 0.04);
            target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0, 1.0, 0),
                    15, 0.3, 0.5, 0.3, 0.03);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 1.3f);
        }
    }

    private void drawArrival(Location origin, Location destination) {
        origin.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin.clone().add(0, 1.0, 0),
                35, 0.4, 0.6, 0.4, 0.05);
        origin.getWorld().spawnParticle(Particle.END_ROD, origin.clone().add(0, 1.0, 0),
                20, 0.35, 0.5, 0.35, 0.04);

        Location center = destination.clone().add(0, 0.15, 0);
        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location point = center.clone().add(x, 0, z);
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(120, 220, 255), 1.2f));
        }

        destination.getWorld().spawnParticle(Particle.END_ROD, destination.clone().add(0, 1.0, 0),
                30, 0.5, 0.7, 0.5, 0.05);
    }

    private static void ensureTracking(Plugin plugin) {
        if (trackingStarted) return;
        trackingStarted = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    Deque<PositionSnapshot> history = HISTORY.computeIfAbsent(
                            online.getUniqueId(), id -> new ArrayDeque<>());
                    history.addLast(new PositionSnapshot(now, online.getLocation().clone()));

                    while (!history.isEmpty()
                            && now - history.peekFirst().time() > MAX_HISTORY_MILLIS) {
                        history.removeFirst();
                    }
                }

                HISTORY.entrySet().removeIf(entry ->
                        plugin.getServer().getPlayer(entry.getKey()) == null);
            }
        }.runTaskTimer(plugin, 0L, SAMPLE_INTERVAL_TICKS);
    }

    @Override
    public String getDescription() {
        return "Teleports you back to where you were " + (historyMillis / 1000L)
                + "s ago and immobilizes enemies within " + radius + " blocks of that location for "
                + (immobilizeDurationMs / 1000.0) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Rewind", (historyMillis / 1000L) + "s"),
                new AbilityStat("Radius", radius + " blocks"),
                new AbilityStat("Immobilize", (immobilizeDurationMs / 1000.0) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
