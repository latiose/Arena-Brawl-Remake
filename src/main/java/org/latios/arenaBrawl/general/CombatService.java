// general/CombatService.java
package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;


public class CombatService {

    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;

    public CombatService(PlayerHealthManager healthManager, ShieldManager shieldManager, DebuffManager debuffManager) {
        this.healthManager = healthManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
    }


    public void applyAbilityDamage(Player attacker, Player victim, double rawDamage, String abilityName) {
        double reduction = shieldManager.getDamageReduction(victim);
        double finalDamage = reduction > 0 ? rawDamage * (1 - reduction) : rawDamage;

        healthManager.damage(victim, finalDamage);
        playDamageFeedback(victim);

        if (debuffManager.hasDebuff(victim, DebuffType.POLYMORPH)) {
            boolean shouldBreak = debuffManager.addAccumulatedDamage(victim, finalDamage, 30.0);
            if (shouldBreak) {
                debuffManager.clear(victim);
            }
        }
        if(!abilityName.equals("Melee")) {
            attacker.sendMessage(String.format(
                    "§7[%s] §fYou dealt §c%.1f §fdamage to §e%s", abilityName, finalDamage, victim.getName()
            ));
            victim.sendMessage(String.format(
                    "§7[%s] §e%s §fdealt §c%.1f §fdamage to you", abilityName, attacker.getName(), finalDamage
            ));
        }
    }

    private void playDamageFeedback(Player victim) {
        victim.playHurtAnimation(0);
        victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1f, 1f);
    }
}