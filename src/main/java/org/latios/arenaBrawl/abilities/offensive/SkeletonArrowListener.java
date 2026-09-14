
package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.EntityCleanupUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class SkeletonArrowListener implements Listener {

    private static final double HITBOX_EXPANSION = 0.12;


    private final Map<UUID, Vector> previousPositions = new HashMap<>();

    private final SkeletonEntityManager skeletonEntityManager;

    public SkeletonArrowListener(
            SkeletonEntityManager skeletonEntityManager
    ) {
        this.skeletonEntityManager = skeletonEntityManager;


        new BukkitRunnable() {
            @Override
            public void run() {
                tickArrowRayTrace();
            }
        }.runTaskTimer(
                ArenaBrawlPlugin.getInstance(),
                1L,
                1L
        );
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {

        if (!(event.getEntity() instanceof LivingEntity shooter)) {
            return;
        }

        if (!skeletonEntityManager.isControlledEntity(shooter)) {
            return;
        }

        if (!(event.getProjectile() instanceof Arrow arrow)) {
            return;
        }

        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

        EntityCleanupUtils.markAsArenaEntity(arrow);


        previousPositions.put(
                arrow.getUniqueId(),
                arrow.getLocation().toVector()
        );
    }


    private void tickArrowRayTrace() {


        Iterator<Map.Entry<UUID, Vector>> iterator =
                previousPositions.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<UUID, Vector> entry = iterator.next();

            UUID arrowId = entry.getKey();
            Vector previousPosition = entry.getValue();

            Entity entity = findEntity(arrowId);

            if (!(entity instanceof Arrow arrow)
                    || arrow.isDead()
                    || !arrow.isValid()) {

                iterator.remove();
                continue;
            }


            if (!(arrow.getShooter() instanceof LivingEntity shooter)
                    || !skeletonEntityManager.isControlledEntity(shooter)) {

                iterator.remove();
                continue;
            }

            Location currentLocation = arrow.getLocation();

            if (!currentLocation.getWorld().equals(
                    getWorldFromEntity(arrow)
            )) {
                iterator.remove();
                continue;
            }

            Vector currentPosition = currentLocation.toVector();


            if (previousPosition.distanceSquared(currentPosition) < 0.000001) {
                continue;
            }

            Player victim = findPlayerHit(
                    arrow,
                    shooter,
                    previousPosition,
                    currentPosition
            );


            entry.setValue(currentPosition.clone());

            if (victim == null) {
                continue;
            }


            handleHit(
                    arrow,
                    shooter,
                    victim
            );


            iterator.remove();
        }
    }


    private Player findPlayerHit(
            Arrow arrow,
            LivingEntity shooter,
            Vector start,
            Vector end
    ) {

        World world = arrow.getWorld();

        BoundingBox searchBox = BoundingBox.of(start, start)
                .union(BoundingBox.of(end, end))
                .expand(HITBOX_EXPANSION);

        Player closestPlayer = null;
        double closestDistanceSquared = Double.MAX_VALUE;

        for (Player player : world.getPlayers()) {

            if (!player.isOnline() || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            Player owner = getOwner(shooter);

            if (owner != null && player.equals(owner)) {
                continue;
            }

            if (owner != null && !skeletonEntityManager.getTeamManager().isEnemy(owner, player)) {
                continue;
            }

            BoundingBox playerBox = player.getBoundingBox().expand(HITBOX_EXPANSION);

            if (!searchBox.overlaps(playerBox)) {
                continue;
            }

            Vector direction = end.clone().subtract(start);
            double length = direction.length();

            if (length <= 0.0) {
                continue;
            }

            direction.normalize();

            org.bukkit.util.RayTraceResult result = playerBox.rayTrace(
                    start,
                    direction,
                    length
            );

            if (result == null) {
                continue;
            }

            double distanceSquared = start.distanceSquared(result.getHitPosition());

            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestPlayer = player;
            }
        }

        return closestPlayer;
    }

    private void handleHit(
            Arrow arrow,
            LivingEntity shooter,
            Player victim
    ) {


        arrow.remove();

        if (!skeletonEntityManager.canAttack(shooter)) {
            return;
        }

        skeletonEntityManager.markAttacked(shooter);

        skeletonEntityManager.onControlledAttack(
                shooter,
                victim
        );

    }


    private Entity findEntity(UUID uuid) {

        for (World world : Bukkit.getWorlds()) {

            Entity entity = world.getEntity(uuid);

            if (entity != null) {
                return entity;
            }
        }

        return null;
    }

    private World getWorldFromEntity(Entity entity) {
        return entity.getWorld();
    }

    private Player getOwner(LivingEntity shooter) {

        UUID ownerId = skeletonEntityManager.getOwner(shooter);

        if (ownerId == null) {
            return null;
        }

        return Bukkit.getPlayer(ownerId);
    }
}
