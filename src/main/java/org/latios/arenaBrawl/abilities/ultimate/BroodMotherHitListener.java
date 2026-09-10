package org.latios.arenaBrawl.abilities.ultimate;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.latios.arenaBrawl.abilities.CooldownManager;

public class BroodMotherHitListener implements Listener {

    private final BroodMotherEntityManager entityManager;
    private final CooldownManager cooldownManager;

    public BroodMotherHitListener(
            BroodMotherEntityManager entityManager,
            CooldownManager cooldownManager
    ) {
        this.entityManager = entityManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!entityManager.isControlledEntity(living)) return;

        event.setCancelled(true);

        if (!(event instanceof EntityDamageByEntityEvent entityEvent)) return;
        if (!(entityEvent.getDamager() instanceof Player attacker)) return;
        if (entityEvent.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        if (cooldownManager.isOnCooldown(attacker, "melee_hit")) {
            return;
        }

        cooldownManager.setCooldown(attacker, "melee_hit", 500);

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