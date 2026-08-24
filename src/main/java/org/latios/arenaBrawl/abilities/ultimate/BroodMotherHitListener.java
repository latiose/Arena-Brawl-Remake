// abilities/ultimate/BroodMotherHitListener.java
package org.latios.arenaBrawl.abilities.ultimate;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BroodMotherHitListener implements Listener {

    private static final long HIT_COOLDOWN_MS = 500;
    private final Map<UUID, Long> lastHitTimes = new HashMap<>();
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

        UUID entityId = living.getUniqueId();
        long now = System.currentTimeMillis();
        long lastHit = lastHitTimes.getOrDefault(entityId, 0L);

        if (now - lastHit < HIT_COOLDOWN_MS) {
            return;
        }

        lastHitTimes.put(entityId, now);
        entityManager.registerHit(living, attacker);
    }

    @EventHandler
    public void onKnockback(EntityKnockbackEvent event) {
        if (entityManager.isControlledEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpiderAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (!entityManager.isControlledEntity(attacker)) return;

        event.setCancelled(true);

        if (!(event.getEntity() instanceof Player victim)) return;

        if (!entityManager.canAttack(attacker)) return;
        entityManager.onControlledAttack(attacker, victim);
        entityManager.markAttacked(attacker);
    }
}