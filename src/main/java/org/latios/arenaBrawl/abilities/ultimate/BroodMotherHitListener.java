// abilities/ultimate/BroodMotherHitListener.java
package org.latios.arenaBrawl.abilities.ultimate;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class BroodMotherHitListener implements Listener {

    private final BroodMotherEntityManager entityManager;

    public BroodMotherHitListener(BroodMotherEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!entityManager.isControlledEntity(living)) return;

        event.setCancelled(true);

        if (!(event instanceof EntityDamageByEntityEvent entityEvent)) return;
        if (!(entityEvent.getDamager() instanceof Player attacker)) return;
        if (entityEvent.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        entityManager.registerHit(living, attacker); // team check now happens inside registerHit
    }

    @EventHandler
    public void onKnockback(EntityKnockbackEvent event) {
        if (entityManager.isControlledEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}