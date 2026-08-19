package org.latios.arenaBrawl.general;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import org.bukkit.persistence.PersistentDataType;

import org.latios.arenaBrawl.abilities.AbilityManager;

import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;

public class CombatListener implements Listener {

    private final TeamManager teamManager;
    private final AbilityManager abilityManager;
    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;

    public CombatListener(TeamManager teamManager, AbilityManager abilityManager, PlayerHealthManager playerHealthManager, ShieldManager shieldManager,DebuffManager debuffManager) {
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
        this.healthManager = playerHealthManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
    }
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        event.setCancelled(true);

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        double damageAmount;
        Player attacker = null;
        String abilityName = "Melee";

        if (event instanceof EntityDamageByEntityEvent entityEvent) {
            attacker = resolveAttacker(entityEvent);
            if (attacker == null) return;
            if (attacker.equals(victim) || teamManager.isAlly(attacker, victim)) return;

            damageAmount = event.getDamage();

            if (entityEvent.getDamager() instanceof Projectile projectile) {
                Double customDamage = projectile.getPersistentDataContainer().get(
                        AbilityItemKeys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE
                );
                String sourceAbility = projectile.getPersistentDataContainer().get(
                        AbilityItemKeys.PROJECTILE_SOURCE_ABILITY, PersistentDataType.STRING
                );

                if (customDamage != null) {
                    damageAmount = customDamage;
                }
                if (sourceAbility != null) {
                    abilityName = sourceAbility;
                }
            }

            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                damageAmount = 10;
                abilityName = "Melee";
            }

            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    && debuffManager.hasDebuff(attacker, DebuffType.POLYMORPH)) {
                debuffManager.clear(attacker);
            }

        } else {
            return;
        }

        double reduction = shieldManager.getDamageReduction(victim);
        if (reduction > 0) {
            damageAmount *= (1 - reduction);
        }

        healthManager.damage(victim, damageAmount);

        playDamageFeedback(victim);
        if(!abilityName.equals("Melee")) {
            attacker.sendMessage(String.format(
                    "§7[%s] §fYou dealt §c%.1f §fdamage to §e%s", abilityName, damageAmount, victim.getName()
            ));
            victim.sendMessage(String.format(
                    "§7[%s] §e%s §fdealt §c%.1f §fdamage to you", abilityName, attacker.getName(), damageAmount
            ));
        }
    }

    private void playDamageFeedback(Player victim) {
        victim.playHurtAnimation(0);
        victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1f, 1f);
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