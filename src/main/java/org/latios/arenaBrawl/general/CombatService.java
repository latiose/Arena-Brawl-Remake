package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.OrbitShieldType;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombatService {

    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final OrbitShieldManager orbitShieldManager;
    private final DamageBuffManager damageBuffManager;
    private final MatchManager matchManager;

    private static final long STAR_SHIELD_EFFECT_DURATION_MILLIS = 4_000;
    private static final List<DebuffType> STAR_SHIELD_POSSIBLE_DEBUFFS =
            List.of(DebuffType.STUN, DebuffType.IMMOBILIZE, DebuffType.SLOW);

    public CombatService(PlayerHealthManager healthManager, ShieldManager shieldManager,
                         DebuffManager debuffManager, OrbitShieldManager orbitShieldManager,
                         DamageBuffManager damageBuffManager, MatchManager matchManager) {
        this.healthManager = healthManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.orbitShieldManager = orbitShieldManager;
        this.damageBuffManager = damageBuffManager;
        this.matchManager = matchManager;
    }

    public void applyAbilityDamage(Player attacker, Player victim, double rawDamage, String abilityName) {

        double multiplier = damageBuffManager.getMultiplier(attacker);

        Match match = matchManager.getActiveMatch();
        if (match != null && match.isDoubleDamageActive()) {
            multiplier *= 2.0;
        }

        double adjustedDamage = rawDamage * multiplier;

        if (orbitShieldManager.hasActiveShield(victim)) {
            OrbitShieldType type = orbitShieldManager.getActiveType(victim);
            if (type != null) {
                orbitShieldManager.consumeCharge(victim);
                resolveShieldEffect(type, attacker, victim);
                return;
            }
        }

        double reduction = shieldManager.getDamageReduction(victim);
        double finalDamage = reduction > 0 ? adjustedDamage * (1 - reduction) : adjustedDamage;

        healthManager.damage(victim, finalDamage, attacker);
        if (abilityName.equals("Melee")) {
            playDamageFeedback(victim);
        }

        if (debuffManager.hasDebuff(victim, DebuffType.POLYMORPH)) {
            boolean shouldBreak = debuffManager.addAccumulatedDamage(victim, finalDamage, 30.0);
            if (shouldBreak) {
                debuffManager.clear(victim);
            }
        }

        if (!abilityName.equals("Melee")) {
            int roundedDamage = (int) Math.round(finalDamage);

            attacker.sendMessage(MessageUtils.positive() + String.format(
                    "§3Your %s hit §3%s §3for §c%d §3damage.",
                    abilityName, victim.getName(), roundedDamage
            ));

            victim.sendMessage(MessageUtils.negative() + String.format(
                    "§3%s's %s hit §3you §3for §c%d §3damage.",
                    attacker.getName(), abilityName, roundedDamage
            ));
        }
    }

    private void resolveShieldEffect(OrbitShieldType type, Player attacker, Player victim) {
        if (type == null) return;
        int remainingCharges = orbitShieldManager.hasActiveShield(victim) ? orbitShieldManager.getCharges(victim) : 0;
        int maxCharges = type.getChargeCount();

        if (remainingCharges > 0) {
            int healthPercent = (int) Math.round(((double) remainingCharges / maxCharges) * 100.0);
            victim.sendMessage(String.format("§e%s Health: %d%%", type.getDisplayName(), healthPercent));
        } else {
            victim.sendMessage(String.format("§eYour %s was destroyed.", type.getDisplayName()));
        }

        if (type.getHealPerCharge() > 0) {
            double healAmount = type.getHealPerCharge();
            healthManager.heal(victim, healAmount);
            int roundedHeal = (int) Math.round(healAmount);

            victim.sendMessage(String.format(
                    "§3Your %s healed you for §a%d §3health.",
                    type.getDisplayName(), roundedHeal
            ));
        }

        if (type.rollsDebuffOnBlock()) {
            applyGuaranteedRandomDebuff(attacker, victim, type);
        }
    }

    private void applyGuaranteedRandomDebuff(Player attacker, Player victim, OrbitShieldType type) {
        List<DebuffType> shuffled = new ArrayList<>(STAR_SHIELD_POSSIBLE_DEBUFFS);
        Collections.shuffle(shuffled);

        for (DebuffType debuffType : shuffled) {
            boolean applied = debuffManager.tryApply(attacker, debuffType, STAR_SHIELD_EFFECT_DURATION_MILLIS);
            if (applied) {
                return;
            }
        }
    }

    private void playDamageFeedback(Player victim) {
        victim.playHurtAnimation(0);
        victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1f, 1f);
    }

    public void applyMinionDamage(Player owner, Player victim, double rawDamage, String sourceName) {
        double ownerMultiplier = damageBuffManager.getMultiplier(owner);
        double reduction = shieldManager.getDamageReduction(victim);

        double finalDamage = rawDamage * ownerMultiplier * (1 - Math.max(0, reduction));
        int roundedDamage = (int) Math.round(finalDamage);

        if (orbitShieldManager.hasActiveShield(victim)) {
            OrbitShieldType type = orbitShieldManager.getActiveType(victim);
            if (type != null) {
                orbitShieldManager.consumeCharge(victim);
                resolveShieldEffect(type, owner, victim);
                return;
            }
        }

        healthManager.damageSilent(victim, finalDamage);

        if (debuffManager.hasDebuff(victim, DebuffType.POLYMORPH)) {
            boolean shouldBreak = debuffManager.addAccumulatedDamage(victim, finalDamage, 30.0);
            if (shouldBreak) {
                debuffManager.clear(victim);
            }
        }

        victim.sendMessage(String.format(
                "%s§3A %s hit §3you §3for §c%d §3damage.",
                MessageUtils.negative(), sourceName, roundedDamage
        ));
    }
}