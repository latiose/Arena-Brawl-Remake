package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

public class KingsGuardTargetListener implements Listener {

    private final KingsGuardEntityManager entityManager;

    public KingsGuardTargetListener(KingsGuardEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!entityManager.isControlledEntity(mob)) return;

        Player expectedTarget = entityManager.getCurrentTarget(mob);

        if (expectedTarget == null) {
            event.setTarget(null);
            return;
        }

        if (event.getTarget() == null || !event.getTarget().equals(expectedTarget)) {
            event.setTarget(expectedTarget);
        }
    }
}
