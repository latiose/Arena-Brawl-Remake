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

import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.team.TeamManager;

public class CombatListener implements Listener {

    private final TeamManager teamManager;
    private final AbilityManager abilityManager;
    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final CombatService combatService;
    private final CooldownManager cooldownManager;
    private final MatchManager matchManager;

    public CombatListener(TeamManager teamManager, AbilityManager abilityManager, PlayerHealthManager playerHealthManager, ShieldManager shieldManager,DebuffManager debuffManager,
                          CombatService combatService, CooldownManager cooldownManager, MatchManager matchManager) {
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
        this.healthManager = playerHealthManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.combatService = combatService;
        this.cooldownManager = cooldownManager;
        this.matchManager = matchManager;
    }


    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        event.setCancelled(true);

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (!(event instanceof EntityDamageByEntityEvent entityEvent)) return;

        Player attacker = resolveAttacker(entityEvent);

        if (attacker == null) return;

        if(!matchManager.isInMatch(attacker) || !matchManager.isInMatch(victim)) {return;}
        if (attacker.equals(victim) || teamManager.isAlly(attacker, victim)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {

            if (cooldownManager.isOnCooldown(attacker, "melee_hit")) {
                return;
            }

            if (debuffManager.hasDebuff(attacker, DebuffType.POLYMORPH)) {
                debuffManager.clear(attacker);
            }

            combatService.applyAbilityDamage(attacker, victim, 10.0, "Melee");
            cooldownManager.setCooldown(attacker, "melee_hit", 500);
            return;
        }


        if (entityEvent.getDamager() instanceof Projectile projectile) {
            Boolean isAoe = projectile.getPersistentDataContainer().has(
                    AbilityItemKeys.PROJECTILE_AOE_RADIUS, PersistentDataType.DOUBLE
            );
            if (isAoe) return;

            Double customDamage = projectile.getPersistentDataContainer().get(
                    AbilityItemKeys.PROJECTILE_DAMAGE, PersistentDataType.DOUBLE
            );
            String sourceAbility = projectile.getPersistentDataContainer().get(
                    AbilityItemKeys.PROJECTILE_SOURCE_ABILITY, PersistentDataType.STRING
            );

            double damageAmount = customDamage != null ? customDamage : event.getDamage();
            String abilityName = sourceAbility != null ? sourceAbility : "Unknown";

            combatService.applyAbilityDamage(attacker, victim, damageAmount, abilityName);
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