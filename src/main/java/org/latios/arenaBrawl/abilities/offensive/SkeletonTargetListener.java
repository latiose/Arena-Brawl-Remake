package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class SkeletonTargetListener implements Listener {

    private final SkeletonEntityManager manager;

    public SkeletonTargetListener(SkeletonEntityManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Skeleton skeleton)) return;
        if (!manager.isControlledEntity(skeleton)) return;

        LivingEntity newTarget = event.getTarget();
        if (newTarget == null) return;

        Player assigned = manager.getCurrentTarget(skeleton);
        boolean allowed = assigned != null
                && newTarget instanceof Player p
                && p.getUniqueId().equals(assigned.getUniqueId());

        if (!allowed) {
            event.setCancelled(true);
        }
    }
}