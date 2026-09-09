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
    private final LifeLeechManager lifeLeechManager;
    private static final long HOLOGRAM_LIFETIME_TICKS = 35;
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
                         DamageBuffManager damageBuffManager, MatchManager matchManager, LifeLeechManager lifeLeechManager,EtherealBodyManager etherealBodyManager,
                         DamageVulnerabilityManager damageVulnerabilityManager) {
        this.healthManager = healthManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.orbitShieldManager = orbitShieldManager;
        this.damageBuffManager = damageBuffManager;
        this.matchManager = matchManager;
        this.lifeLeechManager = lifeLeechManager;
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
/*
        if (abilityName.equals("Melee")) {
            playDamageFeedback(victim);
            if (lifeLeechManager.consumeCharge(attacker)) {
                healthManager.heal(attacker, 60.0, "Life Leech");
            }

            if (Berserk.BERSERK_ACTIVE_PLAYERS.contains(attacker.getUniqueId())) {

                Location center = (impactLocation != null)
                        ? impactLocation.clone()
                        : victim.getLocation().add(0, 1.0, 0);

                BlockData redstoneData = Material.REDSTONE_BLOCK.createBlockData();

                Random random = ThreadLocalRandom.current();

                for (int i = 0; i < 35; i++) {

                    Location particleLoc = center.clone().add(
                            (random.nextDouble() - 0.5) * 0.7,
                            random.nextDouble() * 0.8,
                            (random.nextDouble() - 0.5) * 0.7
                    );

                    double x = (random.nextDouble() - 0.5) * 1.4;
                    double y = 0.5 + random.nextDouble() * 1.2;
                    double z = (random.nextDouble() - 0.5) * 1.4;

                    victim.getWorld().spawnParticle(
                            Particle.BLOCK,
                            particleLoc,
                            0,
                            x,
                            y,
                            z,
                            0.35,
                            redstoneData
                    );
                }
                victim.getWorld().playSound(
                        center,
                        Sound.BLOCK_STONE_BREAK,
                        1.0f,
                        1.1f
                );
            }
        }
    */

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
            healthManager.heal(victim, healAmount, type.getDisplayName());
        }

        if (type.getDamagePerCharge() > 0) {
            double damageAmount = type.getDamagePerCharge();
            int roundedDamage = (int) Math.round(damageAmount);

            healthManager.damage(attacker, damageAmount, victim);
           // playDamageFeedback(attacker);

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
        if(type.doesKnockback()){
            Vector push = attacker.getLocation().toVector().subtract(victim.getLocation().toVector());
            push.setY(0);
            if (push.lengthSquared() < 0.0001) push = new Vector(1, 0, 0);
            push.normalize().multiply(1.3);
            push.setY(0.4);
            attacker.setVelocity(push);
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