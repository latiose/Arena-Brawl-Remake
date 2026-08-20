// general/CombatService.java
package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.OrbitShieldType;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombatService {

    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final OrbitShieldManager orbitShieldManager;

    private static final long STAR_SHIELD_EFFECT_DURATION_MILLIS = 4_000;
    private static final List<DebuffType> STAR_SHIELD_POSSIBLE_DEBUFFS =
            List.of(DebuffType.STUN, DebuffType.IMMOBILIZE, DebuffType.SLOW);
    public CombatService(PlayerHealthManager healthManager, ShieldManager shieldManager,
                         DebuffManager debuffManager, OrbitShieldManager orbitShieldManager) {
        this.healthManager = healthManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.orbitShieldManager = orbitShieldManager;
    }

    public void applyAbilityDamage(Player attacker, Player victim, double rawDamage, String abilityName) {

        // Orbit shield: blocks the hit ENTIRELY and heals the victim instead
        if (orbitShieldManager.hasActiveShield(victim)) {
            OrbitShieldType type = orbitShieldManager.consumeCharge(victim);

            if (type != null) {
                resolveShieldEffect(type, attacker, victim);
                return; // hit is fully blocked either way
            }
        }

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

        if (!abilityName.equals("Melee")) {
            attacker.sendMessage(String.format(
                    "§7[%s] §fYou dealt §c%.1f §fdamage to §e%s", abilityName, finalDamage, victim.getName()
            ));
            victim.sendMessage(String.format(
                    "§7[%s] §e%s §fdealt §c%.1f §fdamage to you", abilityName, attacker.getName(), finalDamage
            ));
        }
    }


    private void resolveShieldEffect(OrbitShieldType type, Player attacker, Player victim) {
        victim.sendMessage(String.format("§fYour %s blocked the hit!", type.getDisplayName()));
        attacker.sendMessage(String.format(
                "§7Your attack was blocked by %s's %s!", victim.getName(), type.getDisplayName()
        ));

        if (type.getHealPerCharge() > 0) {
            double healAmount = type.getHealPerCharge();
            healthManager.heal(victim, healAmount);
            victim.sendMessage(String.format("§a+%.0f HP", healAmount));
        }

        if (type.rollsDebuffOnBlock()) {
            applyGuaranteedRandomDebuff(attacker, victim, type);
        }
    }

    private void applyGuaranteedRandomDebuff(Player attacker, Player victim, OrbitShieldType type) {
        List<DebuffType> shuffled = new ArrayList<>(STAR_SHIELD_POSSIBLE_DEBUFFS);
        Collections.shuffle(shuffled); // random order = each has an equal (~33%) chance of being picked first

        for (DebuffType debuffType : shuffled) {
            boolean applied = debuffManager.tryApply(attacker, debuffType, STAR_SHIELD_EFFECT_DURATION_MILLIS);
            if (applied) {
                attacker.sendMessage(String.format(
                        "§c%s's %s struck you with %s!",
                        victim.getName(), type.getDisplayName(), debuffType.getDisplayName()
                ));
                victim.sendMessage(String.format(
                        "§aYour %s afflicted %s with %s!",
                        type.getDisplayName(), attacker.getName(), debuffType.getDisplayName()
                ));
                return; // exactly one debuff applied, stop here
            }
            // if tryApply failed (attacker already has an active debuff), the shield still
            // consumed a charge and blocked the hit — just no new debuff could stack on top
        }
    }
    private void playDamageFeedback(Player victim) {
        victim.playHurtAnimation(0);
        victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1f, 1f);
    }
}