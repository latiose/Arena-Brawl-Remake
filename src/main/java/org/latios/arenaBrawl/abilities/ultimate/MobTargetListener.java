package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.latios.arenaBrawl.abilities.ultimate.BroodMotherEntityManager;

public class MobTargetListener implements Listener {

    private final BroodMotherEntityManager entityManager;

    public MobTargetListener(BroodMotherEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!entityManager.isControlledEntity(mob)) return;

        Player expectedTarget = entityManager.getCurrentTarget(mob);

        if (expectedTarget == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getTarget() == null || !event.getTarget().equals(expectedTarget)) {
            event.setTarget(expectedTarget);
        }
    }
}