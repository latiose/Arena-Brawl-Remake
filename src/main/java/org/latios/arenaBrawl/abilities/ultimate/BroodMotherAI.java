// abilities/ultimate/BroodMotherAI.java
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BroodMotherAI extends BukkitRunnable {

    private final BroodMotherEntityManager entityManager;

    public BroodMotherAI(BroodMotherEntityManager entityManager) {
        this.entityManager = entityManager;
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
                mob.getPathfinder().moveTo(target.getLocation(), 1.25);
            }
        }
    }
}
