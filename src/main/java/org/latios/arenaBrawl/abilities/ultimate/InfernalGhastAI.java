package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Drives every Infernal Ghast. It keeps the target locked and corrects its
 * flight toward a point above the enemy; the vanilla AI still handles the
 * shooting (and its normal sounds).
 */
public class InfernalGhastAI extends BukkitRunnable {

    private final InfernalGhastEntityManager entityManager;

    public InfernalGhastAI(InfernalGhastEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void run() {
        for (LivingEntity entity : entityManager.getActiveEntities()) {
            if (entity instanceof Ghast ghast && entity.isValid()) {
                entityManager.tick(ghast);
            }
        }
    }
}
