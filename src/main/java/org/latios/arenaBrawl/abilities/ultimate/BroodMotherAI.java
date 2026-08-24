// abilities/ultimate/BroodMotherAI.java
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BroodMotherAI extends BukkitRunnable {

    private static final double ATTACK_RANGE = 2.0;
    private static final double MOVE_SPEED = 0.30;

    private final BroodMotherEntityManager entityManager;

    public BroodMotherAI(BroodMotherEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /*
    @Override
    public void run() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (!entityManager.isControlledEntity(living)) continue;

                Player owner = entityManager.getOwner(living) != null
                        ? Bukkit.getPlayer(entityManager.getOwner(living)) : null;
                if (owner == null) continue;

                Player target = entityManager.getCurrentTarget(living);

                if (target == null) {
                    entityManager.acquireTarget(living, owner,null);
                    continue;
                }
                //entity.setTarget(target);
                if (entityManager.shouldTeleportToTarget(living)) {
                    living.teleport(target.getLocation());
                    entityManager.markTeleported(living);
                    continue;
                }

                double distance = living.getLocation().distance(target.getLocation());

                if (distance <= ATTACK_RANGE) {
                    if (entityManager.canAttack(living)) {
                        entityManager.onControlledAttack(living, target);
                        entityManager.markAttacked(living);
                        living.swingMainHand();
                    }
                    living.setVelocity(new Vector(0, living.getVelocity().getY(), 0)); // stop drifting while attacking
                } else {
                    Vector direction = target.getLocation().toVector()
                            .subtract(living.getLocation().toVector());
                    direction.setY(0); // horizontal movement only; let gravity handle Y naturally
                    if (direction.lengthSquared() > 0.0001) {
                        direction.normalize().multiply(MOVE_SPEED);
                    }
                    living.setVelocity(direction.setY(living.getVelocity().getY()));

                    // Simple jump if blocked directly ahead (basic obstacle handling)
                    if (living.isOnGround() && living.getLocation().add(direction).getBlock().getType().isSolid()) {
                        living.setVelocity(living.getVelocity().setY(0.4));
                    }
                }
            }
        }
    }
    */

    @Override
    public void run() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                if (!entityManager.isControlledEntity(mob)) continue;

                Player owner = entityManager.getOwner(mob) != null
                        ? Bukkit.getPlayer(entityManager.getOwner(mob)) : null;
                if (owner == null) {
                    mob.remove();
                    continue;
                }

                Player target = entityManager.getCurrentTarget(mob);

                if (target == null || !target.isOnline() || target.isDead()) {
                    entityManager.acquireTarget(mob, owner, null);
                    target = entityManager.getCurrentTarget(mob);
                }

                if (target == null) continue;

                if (entityManager.shouldTeleportToTarget(mob)) {
                    mob.teleport(target.getLocation());
                    entityManager.markTeleported(mob);
                }

                if (mob.getTarget() == null || !mob.getTarget().equals(target)) {
                    mob.setTarget(target);
                }
            }
        }
    }
}
