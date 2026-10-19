package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A permanent two-way portal pair linking the caster's position to a targeted ground block.
 * It has no duration limit; the caster's own team can travel through either end. The portal is
 * registered as a structure so it is automatically removed when the match ends.
 */
public class PortalStructure extends PlacedStructure {

    private static final int NONE = 0;
    private static final int END_A = 1;
    private static final int END_B = 2;

    private final Plugin plugin;
    private final TeamManager teamManager;
    private final Location portalA;
    private final Location portalB;
    private final double radius;

    private final Map<UUID, Integer> lastInside = new ConcurrentHashMap<>();

    private BukkitTask task;

    public PortalStructure(Player owner, Location portalA, Location portalB,
                           Plugin plugin, TeamManager teamManager, double radius) {
        super(owner, portalA);
        this.portalA = portalA;
        this.portalB = portalB;
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.radius = radius;
    }

    public void start() {
        // Seed the initial state so nobody standing on a portal at creation time is teleported.
        for (Player player : portalA.getWorld().getPlayers()) {
            lastInside.put(player.getUniqueId(), portalAt(player));
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickPortals();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void tickPortals() {
        for (Player player : portalA.getWorld().getPlayers()) {
            UUID uuid = player.getUniqueId();

            if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR || !canUse(player)) {
                lastInside.put(uuid, NONE);
                continue;
            }

            int current = portalAt(player);
            int previous = lastInside.getOrDefault(uuid, NONE);

            if (current != NONE && current != previous) {
                Location destination = current == END_A ? portalB : portalA;
                int nowStandingAt = current == END_A ? END_B : END_A;

                teleport(player, destination);
                lastInside.put(uuid, nowStandingAt);
            } else {
                lastInside.put(uuid, current);
            }
        }

        drawPortal(portalA);
        drawPortal(portalB);
    }

    private boolean canUse(Player player) {
        if (player.getUniqueId().equals(getOwnerId())) return true;
        Player owner = getOwner();
        return owner != null && teamManager.isAlly(owner, player);
    }

    private int portalAt(Player player) {
        Location point = player.getLocation();
        boolean inA = isInside(point, portalA);
        boolean inB = isInside(point, portalB);

        if (inA && inB) {
            return point.distanceSquared(portalA) <= point.distanceSquared(portalB) ? END_A : END_B;
        }
        if (inA) return END_A;
        if (inB) return END_B;
        return NONE;
    }

    private boolean isInside(Location point, Location portal) {
        if (point.getWorld() == null || !point.getWorld().equals(portal.getWorld())) return false;
        return point.distanceSquared(portal) <= radius * radius;
    }

    private void teleport(Player player, Location destination) {
        Location arrival = destination.clone().setDirection(player.getLocation().getDirection());

        player.teleport(arrival);
        player.setFallDistance(0f);

        arrival.getWorld().spawnParticle(Particle.PORTAL, arrival.clone().add(0, 1.0, 0), 30, 0.3, 0.5, 0.3, 0.4);
        arrival.getWorld().spawnParticle(Particle.END_ROD, arrival.clone().add(0, 1.0, 0), 8, 0.2, 0.5, 0.2, 0.02);
        arrival.getWorld().playSound(arrival, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
    }

    private void drawPortal(Location base) {
        double angleOffset = (System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2;

        for (int i = 0; i < 14; i++) {
            double y = i * 0.18;
            double angle = angleOffset + i * 0.5;
            double x = Math.cos(angle) * 0.55;
            double z = Math.sin(angle) * 0.55;

            base.getWorld().spawnParticle(Particle.PORTAL, base.clone().add(x, y + 0.1, z), 1, 0, 0, 0, 0.1);
        }

        base.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(0, 1.0, 0), 2, 0.25, 0.6, 0.25, 0.0);
    }

    @Override
    public boolean tick() {
        // Never expires on its own; only removed when the match ends or it is replaced.
        return false;
    }

    @Override
    public void remove() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (portalA.getWorld() != null) {
            portalA.getWorld().spawnParticle(Particle.REVERSE_PORTAL, portalA.clone().add(0, 1.0, 0), 20, 0.3, 0.5, 0.3, 0.1);
            portalA.getWorld().playSound(portalA, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.4f);
        }
        if (portalB.getWorld() != null) {
            portalB.getWorld().spawnParticle(Particle.REVERSE_PORTAL, portalB.clone().add(0, 1.0, 0), 20, 0.3, 0.5, 0.3, 0.1);
        }

        lastInside.clear();
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return List.of();
    }
}
