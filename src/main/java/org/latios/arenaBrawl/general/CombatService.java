package org.latios.arenaBrawl.general;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.support.LifeLeechManager;
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
import java.util.concurrent.ThreadLocalRandom;

public class CombatService {

    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final OrbitShieldManager orbitShieldManager;
    private final DamageBuffManager damageBuffManager;
    private final MatchManager matchManager;
    private final LifeLeechManager lifeLeechManager;
    private static final long HOLOGRAM_LIFETIME_TICKS = 35;
    private static final long STAR_SHIELD_EFFECT_DURATION_MILLIS = 4_000;
    private static final List<DebuffType> STAR_SHIELD_POSSIBLE_DEBUFFS =
            List.of(DebuffType.STUN, DebuffType.IMMOBILIZE, DebuffType.SLOW);

    public CombatService(PlayerHealthManager healthManager, ShieldManager shieldManager,
                         DebuffManager debuffManager, OrbitShieldManager orbitShieldManager,
                         DamageBuffManager damageBuffManager, MatchManager matchManager, LifeLeechManager lifeLeechManager) {
        this.healthManager = healthManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.orbitShieldManager = orbitShieldManager;
        this.damageBuffManager = damageBuffManager;
        this.matchManager = matchManager;
        this.lifeLeechManager = lifeLeechManager;
    }

    public void applyAbilityDamage(Player attacker, Player victim, double rawDamage, String abilityName) {
        applyAbilityDamage(attacker, victim, rawDamage, abilityName, null);
    }

    public void applyAbilityDamage(Player attacker, Player victim, double rawDamage, String abilityName, Location impactLocation) {
        if (victim == null || victim.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        double multiplier = damageBuffManager.getMultiplier(attacker);

        Match match = matchManager.getMatchFor(attacker);
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
            if (lifeLeechManager.consumeCharge(attacker)) {
                healthManager.heal(attacker, 60.0);
                attacker.sendMessage(MessageUtils.positive() + "§3Your Life Leech healed you for §a60 §3health!");
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
            double healAmount = type.getHealPerCharge();
            healthManager.heal(victim, healAmount);
            int roundedHeal = (int) Math.round(healAmount);

            victim.sendMessage(String.format(
                    "§3Your %s healed you for §a%d §3health.",
                    type.getDisplayName(), roundedHeal
            ));
        }

        if (type.getDamagePerCharge() > 0) {
            double damageAmount = type.getDamagePerCharge();
            int roundedDamage = (int) Math.round(damageAmount);

            healthManager.damage(attacker, damageAmount, victim);
            playDamageFeedback(attacker);

            Location impactLoc = attacker.getLocation().add(0, attacker.getHeight() * 0.5, 0);
            spawnHologram(attacker, String.valueOf(roundedDamage), impactLoc);

            victim.sendMessage(MessageUtils.positive() + String.format(
                    "§3Your %s hit §3%s §3for §c%d §3damage.",
                    type.getDisplayName(), attacker.getName(), roundedDamage
            ));

            attacker.sendMessage(MessageUtils.negative() + String.format(
                    "§3%s's %s hit §3you §3for §c%d §3damage.",
                    victim.getName(), type.getDisplayName(), roundedDamage
            ));
        }

        if (type.rollsDebuffOnBlock()) {
            applyGuaranteedRandomDebuff(attacker, type);
        }
    }

    private void applyGuaranteedRandomDebuff(Player attacker, OrbitShieldType type) {
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

    public void applyMinionDamage(Player owner, Player victim, double rawDamage, String sourceName, Location impactLocation) {
        if (victim == null || victim.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

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
}