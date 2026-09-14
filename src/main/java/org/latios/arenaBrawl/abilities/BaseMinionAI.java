package org.latios.arenaBrawl.abilities;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BaseMinionAI extends BukkitRunnable {

    private final BaseMinionManager entityManager;
    private final double speedMultiplier;

    public BaseMinionAI(BaseMinionManager entityManager, double speedMultiplier) {
        this.entityManager = entityManager;
        this.speedMultiplier = speedMultiplier;
    }

    public BaseMinionAI(BaseMinionManager entityManager) {
        this(entityManager, 1.25);
    }

    @Override
    public void run() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                if (!entityManager.isControlledEntity(mob)) continue;

                Player owner = entityManager.getOwner(mob) != null
                        ? Bukkit.getPlayer(entityManager.getOwner(mob)) : null;

                if (owner == null || !owner.isOnline()) {
                    mob.remove();
                    continue;
                }

                Player target = entityManager.getCurrentTarget(mob);

                if (target == null || !target.isOnline() || target.isDead() || target.getGameMode() == GameMode.SPECTATOR) {
                    entityManager.acquireTarget(mob, owner, null);
                    target = entityManager.getCurrentTarget(mob);
                }

                if (target == null) {
                    mob.setTarget(null);
                    mob.getPathfinder().stopPathfinding();
                    continue;
                }

                if (entityManager.shouldTeleportToTarget(mob)) {
                    mob.teleport(target.getLocation());
                    entityManager.markTeleported(mob);
                }

                mob.setTarget(target);
                mob.getPathfinder().moveTo(target.getLocation(), speedMultiplier);
            }
        }
    }
}