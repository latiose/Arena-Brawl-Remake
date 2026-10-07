package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MeleeHitEffect;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Grit implements Ability, MeleeHitEffect {

    private static final String COOLDOWN_KEY = "grit";
    private static final String SHIELD_TITLE = "GRIT";

    private final AbilityCost cost;
    private final ShieldManager shieldManager;
    private final long cooldownMs;
    private final double dashSpeed;
    private final double reductionPerUse;
    private final double maxReduction;
    private final long durationMs;
    private final long reductionPerHitMs;
    private final CooldownManager cooldownManager;
    public Grit(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                ShieldManager shieldManager, AbilityConfig config) {
        this.shieldManager = shieldManager;
        this.cooldownMs = config.getLong("cooldown-ms", 24000L);
        this.dashSpeed = config.getDouble("dash-speed", 0.8);
        this.reductionPerUse = config.getDouble("reduction-per-use", 0.10);
        this.maxReduction = config.getDouble("max-reduction", 0.70);
        this.durationMs = config.getLong("duration-ms", 10000L);
        this.reductionPerHitMs = config.getLong("reduction-per-hit-ms", 2000L);
        this.cost = new CooldownCost(cooldownManager, COOLDOWN_KEY, cooldownMs, upgradeManager);
        this.cooldownManager = cooldownManager;
    }

    @Override
    public String getName() {
        return "Grit";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Dash slightly forward, reducing damage taken by "
                + Math.round(reductionPerUse * 100) + "% for each use, up to "
                + Math.round(maxReduction * 100) + "%. The effect falls off entirely after "
                + (durationMs / 1000L) + " seconds without being used."
                + "Melee hits decrease the cooldown by "+ (reductionPerHitMs/1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Damage Reduction per Use", Math.round(reductionPerUse * 100) + "%"),
                new AbilityStat("Max Damage Reduction", Math.round(maxReduction * 100) + "%"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Vector dir = player.getLocation().getDirection().normalize().setY(0.1).multiply(dashSpeed);
        player.setVelocity(dir);
        player.setFallDistance(0);

        double current = shieldManager.getReductionByTitle(player, SHIELD_TITLE);
        double newReduction = Math.min(maxReduction, current + reductionPerUse);

        shieldManager.removeByTitle(player, SHIELD_TITLE);
        shieldManager.applyShield(player, newReduction, durationMs, SHIELD_TITLE);

        shieldManager.getReductionByTitle(player, SHIELD_TITLE);

        int percent = (int) Math.round(newReduction * 100);
        player.sendMessage("§3Grit: §a" + percent + "% §3damage reduction.");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2.5f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 2.5f, 0.8f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);

        return true;
    }

    @Override
    public void onMatchStart(Player player) {
        Ability.super.onMatchStart(player);
    }


    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        cooldownManager.reduceCooldown(attacker, COOLDOWN_KEY, reductionPerHitMs);
    }

    @Override
    public double getMultiplier(Player attacker) {
        return 1.0;
    }


}