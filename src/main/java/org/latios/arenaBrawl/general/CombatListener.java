package org.latios.arenaBrawl.general;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.abilities.ultimate.ShieldWall;
import org.latios.arenaBrawl.team.TeamManager;

public class CombatListener implements Listener {

    private final TeamManager teamManager;
    private final AbilityManager abilityManager;
    private final PlayerHealthManager healthManager;

    public CombatListener(TeamManager teamManager, AbilityManager abilityManager, PlayerHealthManager playerHealthManager) {
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
        this.healthManager = playerHealthManager;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        event.setCancelled(true);

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        double damageAmount;

        if (event instanceof EntityDamageByEntityEvent entityEvent) {
            Player attacker = resolveAttacker(entityEvent);
            if (attacker == null) return;
            if (attacker.equals(victim) || teamManager.isAlly(attacker, victim)) return;

            damageAmount = event.getDamage();


            if (entityEvent.getDamager() instanceof Projectile projectile) {
                Double customDamage = projectile.getPersistentDataContainer().get(
                        AbilityItemKeys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE
                );
                if (customDamage != null) {
                    damageAmount = customDamage;
                }
            }

            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                damageAmount = 10;
            }
        } else {
            return;
        }

        Ability ultimate = abilityManager.getAbility(victim, AbilitySlot.ULTIMATE);
        if (ultimate instanceof ShieldWall shieldWall) {
            double reduction = shieldWall.getDamageReduction(victim);
            if (reduction > 0) {
                damageAmount *= (1 - reduction);
            }
        }

        healthManager.damage(victim, damageAmount);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler
    public void onKnockback(EntityKnockbackEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }


}