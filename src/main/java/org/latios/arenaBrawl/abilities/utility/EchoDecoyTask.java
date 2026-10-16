package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves an Echo Decoy forward, mimicking its owner. Enemies that get close are
 * "baited" as it bursts. Once it has been out for a moment it will also surprise
 * nearby enemies, stunning everyone caught in a wider radius.
 */
public class EchoDecoyTask extends BukkitRunnable {

    private final ArmorStand decoy;
    private final Player owner;
    private final Vector direction;
    private final double moveSpeed;
    private final double baitRadius;
    private final double surpriseRadius;
    private final int armDelayTicks;
    private final int durationTicks;
    private final long stunDurationMs;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    private int ticks = 0;

    public EchoDecoyTask(ArmorStand decoy, Player owner, Vector direction, double moveSpeed,
                         double baitRadius, double surpriseRadius, long armDelayMs, long durationMs,
                         long stunDurationMs, TeamManager teamManager, DebuffManager debuffManager) {
        this.decoy = decoy;
        this.owner = owner;
        this.direction = direction;
        this.moveSpeed = moveSpeed;
        this.baitRadius = baitRadius;
        this.surpriseRadius = surpriseRadius;
        this.armDelayTicks = (int) Math.max(0, armDelayMs / 50L);
        this.durationTicks = (int) Math.max(1, durationMs / 50L);
        this.stunDurationMs = stunDurationMs;
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public void run() {
        ticks++;

        if (!owner.isOnline() || !decoy.isValid() || ticks > durationTicks) {
            vanish(false);
            return;
        }

        Location next = decoy.getLocation().add(direction.clone().multiply(moveSpeed));
        next.setYaw(decoy.getLocation().getYaw());
        decoy.teleport(next);

        decoy.getWorld().spawnParticle(Particle.CLOUD, decoy.getLocation().add(0, 1, 0), 3,
                0.2, 0.4, 0.2, 0.01);
        decoy.getWorld().spawnParticle(Particle.SCULK_SOUL, decoy.getLocation().add(0, 1, 0), 1,
                0.15, 0.3, 0.15, 0.0);

        // Once armed, catching anyone in the wider radius stuns them all.
        if (ticks >= armDelayTicks) {
            List<Player> caught = enemiesWithin(surpriseRadius);
            if (!caught.isEmpty()) {
                stunCaught(caught);
                vanish(true);
                return;
            }
        }

        // Keep the original bait: vanishing when an enemy gets right up to it.
        if (!enemiesWithin(baitRadius).isEmpty()) {
            vanish(true);
        }
    }

    private List<Player> enemiesWithin(double radius) {
        List<Player> found = new ArrayList<>();
        double radiusSquared = radius * radius;
        for (Player candidate : decoy.getWorld().getPlayers()) {
            if (candidate.equals(owner) || candidate.isDead()) continue;
            if (!teamManager.isEnemy(owner, candidate)) continue;
            if (candidate.getLocation().distanceSquared(decoy.getLocation()) <= radiusSquared) {
                found.add(candidate);
            }
        }
        return found;
    }

    private void stunCaught(List<Player> caught) {
        for (Player victim : caught) {
            debuffManager.tryApply(owner, victim, DebuffType.STUN, stunDurationMs);
            victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, victim.getLocation().add(0, 1, 0),
                    18, 0.3, 0.6, 0.3, 0.1);
        }
    }

    private void vanish(boolean baited) {
        cancel();

        if (decoy.isValid()) {
            Location at = decoy.getLocation().add(0, 1, 0);
            at.getWorld().spawnParticle(Particle.SCULK_SOUL, at, baited ? 30 : 12, 0.3, 0.5, 0.3, 0.03);
            at.getWorld().spawnParticle(Particle.CLOUD, at, baited ? 20 : 8, 0.3, 0.4, 0.3, 0.02);
            at.getWorld().playSound(at, baited ? Sound.ENTITY_ILLUSIONER_MIRROR_MOVE : Sound.ENTITY_ENDERMAN_TELEPORT,
                    0.8f, baited ? 0.8f : 1.4f);
            decoy.remove();
        }
    }
}
