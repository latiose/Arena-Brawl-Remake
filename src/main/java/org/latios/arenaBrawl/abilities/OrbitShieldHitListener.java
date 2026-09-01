package org.latios.arenaBrawl.abilities;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

public class OrbitShieldHitListener implements Listener {

    private final OrbitShieldManager orbitShieldManager;
    private final CombatService combatService;
    private final TeamManager teamManager;
    private final CooldownManager cooldownManager;
    private final DebuffManager debuffManager;
    public OrbitShieldHitListener(OrbitShieldManager orbitShieldManager, CombatService combatService,
                                  TeamManager teamManager, CooldownManager cooldownManager,DebuffManager debuffManager) {
        this.orbitShieldManager = orbitShieldManager;
        this.combatService = combatService;
        this.teamManager = teamManager;
        this.cooldownManager = cooldownManager;
        this.debuffManager = debuffManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (processShieldHit(attacker, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (processShieldHit(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player attacker = event.getPlayer();
        if (attacker.getGameMode() == GameMode.SPECTATOR) return;

        double maxDistance = 3.5;
        attacker.getWorld().getNearbyEntities(attacker.getEyeLocation(), maxDistance, maxDistance, maxDistance)
                .stream()
                .filter(e -> orbitShieldManager.getOwnerOfChargeEntity(e) != null)
                .filter(e -> e.getBoundingBox().expand(0.3).contains(
                        attacker.getEyeLocation().toVector().add(attacker.getEyeLocation().getDirection().multiply(attacker.getEyeLocation().distance(e.getLocation())))
                ))
                .findFirst().ifPresent(targetEntity -> processShieldHit(attacker, targetEntity));

    }

    private boolean processShieldHit(Player attacker, Entity target) {
        Player owner = orbitShieldManager.getOwnerOfChargeEntity(target);
        if (owner == null) return false;

        if (attacker.equals(owner) || attacker.getGameMode() == GameMode.SPECTATOR) return true;

        if (teamManager.isAlly(attacker, owner)) return true;

        if (debuffManager.hasDebuff(attacker, DebuffType.STUN)) {
            return true;
        }

        if (cooldownManager.isOnCooldown(attacker, "melee_hit")) {
            return true;
        }
        cooldownManager.setCooldown(attacker, "melee_hit", 500);

        OrbitShieldType type = orbitShieldManager.consumeCharge(owner);
        if (type != null) {
            combatService.resolveShieldEffect(type, attacker, owner);
        }
        return true;
    }
}