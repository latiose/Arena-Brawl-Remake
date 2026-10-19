package org.latios.arenaBrawl.general;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.OrbitShieldType;
import org.latios.arenaBrawl.abilities.support.*;
import org.latios.arenaBrawl.abilities.ultimate.Berserk;
import org.latios.arenaBrawl.abilities.ultimate.BloodMoon;
import org.latios.arenaBrawl.abilities.utility.Counter;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CombatService {

    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final OrbitShieldManager orbitShieldManager;
    private final DamageBuffManager damageBuffManager;
    private final MatchManager matchManager;
    private static final long HOLOGRAM_LIFETIME_TICKS = 30;
    private static final long STAR_SHIELD_EFFECT_DURATION_MILLIS = 4_000;
    private static final List<DebuffType> STAR_SHIELD_POSSIBLE_DEBUFFS =
            List.of(DebuffType.STUN, DebuffType.IMMOBILIZE, DebuffType.SLOW);
    private final EtherealBodyManager etherealBodyManager;
    private final DamageVulnerabilityManager damageVulnerabilityManager;
    private final List<MeleeHitEffect> meleeHitEffects = new ArrayList<>();

    public void registerMeleeHitEffect(MeleeHitEffect effect) {
        meleeHitEffects.add(effect);
    }

    public CombatService(PlayerHealthManager healthManager, ShieldManager shieldManager,
                         DebuffManager debuffManager, OrbitShieldManager orbitShieldManager,
                         DamageBuffManager damageBuffManager, MatchManager matchManager, EtherealBodyManager etherealBodyManager,
                         DamageVulnerabilityManager damageVulnerabilityManager) {
        this.healthManager = healthManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.orbitShieldManager = orbitShieldManager;
        this.damageBuffManager = damageBuffManager;
        this.matchManager = matchManager;
        this.etherealBodyManager = etherealBodyManager;
        this.damageVulnerabilityManager = damageVulnerabilityManager;
    }

    public void applyAbilityDamage(Player attacker, Player victim, double rawDamage, String abilityName) {
        applyAbilityDamage(attacker, victim, rawDamage, abilityName, null);
    }

    public void applyAbilityDamage(Player attacker, Player victim, double rawDamage, String abilityName, Location impactLocation) {
        if (victim == null || victim.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        double multiplier = damageBuffManager.getMultiplier(attacker);

        if (abilityName.equals("Melee")) {
            for (MeleeHitEffect effect : meleeHitEffects) {
                double effectMultiplier = effect.getMultiplier(attacker);
                multiplier *= effectMultiplier;
            }
        }

        Match match = matchManager.getMatchFor(attacker);
        if (match != null && match.isDoubleDamageActive()) {
            multiplier *= 2.0;
        }

        double adjustedDamage = rawDamage * multiplier;

        Counter.CounterState counter = Counter.consumeActive(victim);
        if (counter != null) {
            double counterReduction = shieldManager.getDamageReduction(victim);
            double reflectedDamage = counterReduction > 0 ? adjustedDamage * (1 - counterReduction) : adjustedDamage;

            double counterVulnerability = damageVulnerabilityManager.getBonusMultiplier(victim);
            if (counterVulnerability > 0) {
                reflectedDamage *= (1 + counterVulnerability);
            }

            Location counterLoc = victim.getLocation().add(0, 1.0, 0);
            victim.getWorld().spawnParticle(Particle.ENCHANTED_HIT, counterLoc, 25, 0.3, 0.5, 0.3, 0.1);
            victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK, counterLoc, 4, 0.4, 0.4, 0.4, 0.0);
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.2f, 1.0f);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.4f);

            if (attacker != null && !attacker.equals(victim)) {
                int roundedReflected = (int) Math.round(reflectedDamage);

                healthManager.damage(attacker, reflectedDamage, victim);
                spawnHologram(attacker, String.valueOf(roundedReflected),
                        attacker.getLocation().add(0, attacker.getHeight() * 0.5, 0));

                victim.sendMessage(MessageUtils.positive() + String.format(
                        "§3Your %s hit §3%s §3for §c%d §3damage.",
                        "Counter", attacker.getName(), roundedReflected
                ));
                attacker.sendMessage(MessageUtils.negative() + String.format(
                        "§3%s's %s hit §3you §3for §c%d §3damage.",
                        victim.getName(), "Counter", roundedReflected
                ));
            }
            return;
        }

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

        double vulnerabilityBonus = damageVulnerabilityManager.getBonusMultiplier(victim);
        if (vulnerabilityBonus > 0) {
            finalDamage *= (1 + vulnerabilityBonus);
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.0, 0), 10, 0.3, 0.5, 0.3, 0.1);
        }

        etherealBodyManager.processIncomingDamage(victim, finalDamage);

        UUID casterUUID = LifeBond.ACTIVE_BONDS.get(victim.getUniqueId());
        if (casterUUID != null) {
            Player caster = Bukkit.getPlayer(casterUUID);

            if (caster != null && caster.isOnline() && !caster.isDead()) {
                double redirectedDamage = finalDamage * 0.30;
                finalDamage = finalDamage * 0.70;

                healthManager.damage(caster, redirectedDamage, attacker);
                caster.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, caster.getLocation().add(0, 1, 0), 3);

                caster.sendMessage(MessageUtils.negative() + String.format(
                        "§3You absorbed §c%d §3damage for §a%s §3via Life Bond!",
                        (int) Math.round(redirectedDamage), victim.getName()
                ));

                victim.sendMessage(MessageUtils.positive() + String.format(
                        "§a%s §3absorbed §c%d §3of your damage via Life Bond!",
                        caster.getName(), (int) Math.round(redirectedDamage)
                ));
            }
        }

        healthManager.damage(victim, finalDamage, attacker);
        if (abilityName.equals("Melee")) {
            Thornheart.onMeleeDamageTaken(victim, attacker, healthManager);
        }
        Bloodthirst.onDamageDealt(attacker, victim, finalDamage, healthManager);
        BloodMoon.onDamageDealt(attacker, victim, finalDamage, healthManager);
        if (abilityName.equals("Melee")) {
            playDamageFeedback(victim);

            Location impactLoc = impactLocation != null ? impactLocation : victim.getLocation().add(0, 1.0, 0);
            for (MeleeHitEffect effect : meleeHitEffects) {
                effect.onMeleeHit(attacker, victim, finalDamage, impactLoc);
            }
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

        Location resolvedImpact = impactLocation != null ? impactLocation : estimateMeleeImpact(attacker, victim);
        spawnHologram(victim, String.valueOf((int) Math.round(finalDamage)), resolvedImpact);
    }

    private Location estimateMeleeImpact(Player attacker, Player victim) {
        Location eye = attacker.getEyeLocation();
        org.bukkit.util.Vector direction = eye.getDirection();
        org.bukkit.util.BoundingBox box = victim.getBoundingBox();

        org.bukkit.util.RayTraceResult result = box.rayTrace(eye.toVector(), direction, 6.0);

        if (result != null) {
            return result.getHitPosition().toLocation(victim.getWorld());
        }

        return victim.getLocation().add(0, victim.getHeight() * 0.5, 0);
    }

    public void resolveShieldEffect(OrbitShieldType type, Player attacker, Player victim) {
        resolveShieldEffect(type, attacker, victim, false);
    }


    private void resolveShieldEffect(OrbitShieldType type, Player attacker, Player victim, boolean reflected) {
        if (type == null) return;

        int remainingCharges = orbitShieldManager.hasActiveShield(victim) ? orbitShieldManager.getCharges(victim) : 0;
        int maxCharges = type.getChargeCount();
        if (remainingCharges > 0) {
            int healthPercent = (int) Math.round(((double) remainingCharges / maxCharges) * 100.0);
            victim.sendMessage(String.format("§e%s Health: §a%d%%", type.getDisplayName(), healthPercent));
        } else {
            victim.sendMessage(String.format("§eYour %s was destroyed.", type.getDisplayName()));
        }

        if (type.getHealPerCharge() > 0) {
            healVictim(victim, type.getHealPerCharge(), type.getDisplayName());
        }


        if (type.isRandom()) {
            org.latios.arenaBrawl.general.GamblerEffects.Entry result = org.latios.arenaBrawl.general.GamblerEffects.roll();
            victim.sendMessage("§6" + type.getDisplayName() + " rolled: §e" + result.name());
            result.effect().apply(this, attacker, victim, reflected);
        }

        if (type.getDamagePerCharge() > 0) {
            reflectDamage(attacker, victim, type.getDamagePerCharge(), type.getDisplayName(), reflected);
        }
        if (type.rollsDebuffOnBlock()) {
            applyGuaranteedRandomDebuff(attacker);
        }
        if (type.doesKnockback()) {
            applyKnockbackEffect(attacker, victim);
        }
    }

    private List<DebuffType> getRandomDebuffPriorityOrder() {
        double roll = ThreadLocalRandom.current().nextDouble();
        DebuffType primary;

        if (roll < 0.50) {
            primary = DebuffType.SLOW;
        } else if (roll < 0.75) {
            primary = DebuffType.STUN;
        } else {
            primary = DebuffType.IMMOBILIZE;
        }

        List<DebuffType> order = new ArrayList<>();
        order.add(primary);

        List<DebuffType> remaining = new ArrayList<>(STAR_SHIELD_POSSIBLE_DEBUFFS);
        remaining.remove(primary);
        Collections.shuffle(remaining);
        order.addAll(remaining);

        return order;
    }

    private void playDamageFeedback(Player victim) {
        victim.playHurtAnimation(90f);
        victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1f, 1f);
    }

    public void applyMinionDamage(Player owner, Player victim, double rawDamage, String sourceName, Location impactLocation) {
        if (victim == null || victim.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        double ownerMultiplier = damageBuffManager.getMultiplier(owner);
        double reduction = shieldManager.getDamageReduction(victim);

        double finalDamage = rawDamage * ownerMultiplier * (1 - Math.max(0, reduction));
        double vulnerabilityBonus = damageVulnerabilityManager.getBonusMultiplier(victim);
        if (vulnerabilityBonus > 0) {
            finalDamage *= (1 + vulnerabilityBonus);
        }
        int roundedDamage = (int) Math.round(finalDamage);

        if (orbitShieldManager.hasActiveShield(victim)) {
            OrbitShieldType type = orbitShieldManager.getActiveType(victim);
            if (type != null) {
                orbitShieldManager.consumeCharge(victim);
                resolveShieldEffect(type, owner, victim);
                return;
            }
        }

        healthManager.damageSilent(victim, finalDamage, owner);
        Bloodthirst.onDamageDealt(owner, victim, finalDamage, healthManager);
        BloodMoon.onDamageDealt(owner, victim, finalDamage, healthManager);
        if (debuffManager.hasDebuff(victim, DebuffType.POLYMORPH)) {
            boolean shouldBreak = debuffManager.addAccumulatedDamage(victim, finalDamage, 30.0);
            if (shouldBreak) {
                debuffManager.clear(victim);
            }
        }

        owner.sendMessage(MessageUtils.positive() + String.format(
                "§3Your %s hit §3%s §3for §c%d §3damage.",
                sourceName, victim.getName(), roundedDamage
        ));

        victim.sendMessage(MessageUtils.negative() + String.format(
                "§3%s's %s hit §3you §3for §c%d §3damage.",
                owner.getName(), sourceName, roundedDamage
        ));

        Location resolvedImpact = impactLocation != null ? impactLocation : victim.getLocation().add(0, victim.getHeight() * 0.5, 0);
        spawnHologram(victim, String.valueOf(roundedDamage), resolvedImpact);
    }

    private void spawnHologram(Player victim, String phrase, Location impactLocation) {
        float jitterX = (float) ThreadLocalRandom.current().nextDouble(-0.15, 0.15);
        float jitterY = (float) ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
        float jitterZ = (float) ThreadLocalRandom.current().nextDouble(-0.15, 0.15);

        Location spawnLoc = impactLocation.clone();
        TextColor color = TextColor.color(0xFF474C);

        TextDisplay hologram = victim.getWorld().spawn(spawnLoc, TextDisplay.class, d -> {
            d.text(Component.text(phrase, color));
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(true);
            d.setShadowed(true);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(100, 40, 0, 10));

            Transformation currentTransform = d.getTransformation();
            Vector3f translation = new Vector3f(jitterX, jitterY, jitterZ);

            d.setTransformation(new Transformation(
                    translation,
                    currentTransform.getLeftRotation(),
                    currentTransform.getScale(),
                    currentTransform.getRightRotation()
            ));
        });
        EntityCleanupUtils.markAsArenaEntity(hologram);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (hologram.isDead()) {
                    cancel();
                    return;
                }

                if (ticksElapsed >= HOLOGRAM_LIFETIME_TICKS) {
                    hologram.remove();
                    cancel();
                    return;
                }

                hologram.teleport(hologram.getLocation().add(0, 0.02, 0));
                ticksElapsed++;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
    }


    void reflectDamage(Player attacker, Player victim, double baseDamage, String sourceName, boolean reflected) {
        double damageAmount = baseDamage
                * damageBuffManager.getMultiplier(victim)
                * (1.0 + damageVulnerabilityManager.getBonusMultiplier(attacker));
        int roundedDamage = (int) Math.round(damageAmount);

        OrbitShieldType attackerShield = (!reflected && orbitShieldManager.hasActiveShield(attacker))
                ? orbitShieldManager.getActiveType(attacker)
                : null;

        if (attackerShield != null) {
            orbitShieldManager.consumeCharge(attacker);
            resolveShieldEffect(attackerShield, victim, attacker, true);
            return;
        }

        healthManager.damage(attacker, damageAmount, victim);
        spawnHologram(attacker, String.valueOf(roundedDamage),
                attacker.getLocation().add(0, attacker.getHeight() * 0.5, 0));

        victim.sendMessage(MessageUtils.positive() + String.format(
                "§3Your %s hit §3%s §3for §c%d §3damage.", sourceName, attacker.getName(), roundedDamage));
        attacker.sendMessage(MessageUtils.negative() + String.format(
                "§3%s's %s hit §3you §3for §c%d §3damage.", victim.getName(), sourceName, roundedDamage));
    }

    void applyKnockbackEffect(Player attacker, Player victim) {
        Vector push = attacker.getLocation().toVector().subtract(victim.getLocation().toVector());
        push.setY(0);
        if (push.lengthSquared() < 0.0001) push = new Vector(1, 0, 0);
        push.normalize().multiply(1.3);
        push.setY(0.4);
        attacker.setVelocity(push);
    }

    void healVictim(Player victim, double amount, String source) {
        healthManager.heal(victim, amount, source);
    }

    void applyGuaranteedRandomDebuff(Player attacker) {
        for (DebuffType debuffType : getRandomDebuffPriorityOrder()) {
            if (debuffManager.tryApply(attacker, debuffType, STAR_SHIELD_EFFECT_DURATION_MILLIS)) return;
        }
    }
}