package org.latios.arenaBrawl.abilities.support;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Heals an ally at the cost of the caster's own health. The self-cost can never
 * kill the caster.
 */
public class BloodOffering implements Ability {

    private final long cooldownMs;
    private final double healAmount;
    private final double selfCost;
    private final double maxRange;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public BloodOffering(CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                         TeamManager teamManager, PlayerHealthManager healthManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 25_000L);
        this.healAmount = config.getDouble("heal-amount", 450.0);
        this.selfCost = config.getDouble("self-cost", 250.0);
        this.maxRange = config.getDouble("max-range", 12.0);

        this.cost = new CooldownCost(cooldownManager, "bloodoffering", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Blood Offering";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player ally = findAlly(player);
        if (ally == null) return false;

        double healed = healthManager.healAlly(player, ally, healAmount,getName());
        if (healed <= 0.0) return false;


        double available = Math.max(0.0, healthManager.getHealth(player) - 1.0);
        double costPaid = Math.min(selfCost, available);
        if (costPaid > 0.0) {
            healthManager.damageSilent(player, costPaid, null);
        }

        Location center = ally.getLocation().add(0, 1.0, 0);
        center.getWorld().spawnParticle(Particle.HEART, center, 12, 0.35, 0.45, 0.35, 0.0);
        center.getWorld().spawnParticle(Particle.DUST, center, 20, 0.4, 0.5, 0.4,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 20, 20), 1.3f));
        center.getWorld().playSound(center, Sound.ITEM_BOTTLE_FILL, 1.0f, 0.7f);

        Location casterLoc = player.getLocation().add(0, 1.0, 0);
        casterLoc.getWorld().spawnParticle(Particle.DUST, casterLoc, 18, 0.35, 0.5, 0.35,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(120, 10, 10), 1.3f));
        casterLoc.getWorld().playSound(casterLoc, Sound.ENTITY_WITCH_DRINK, 0.8f, 1.2f);

        return true;
    }

    private Player findAlly(Player caster) {
        Location eye = caster.getEyeLocation();
        RayTraceResult result = caster.getWorld().rayTrace(
                eye, eye.getDirection(), maxRange, FluidCollisionMode.NEVER, true, 0.6,
                entity -> entity instanceof Player target
                        && !target.equals(caster)
                        && !target.isDead()
                        && target.getGameMode() != GameMode.SPECTATOR
                        && config.getMatchManager().isInMatch(target)
                        && teamManager.isAlly(caster, target)
        );

        if (result != null && result.getHitEntity() instanceof Player ally) {
            return ally;
        }
        caster.sendMessage(MessageUtils.noValidPlayer());
        return null;
    }

    @Override
    public String getDescription() {
        return "Heals an ally for " + (int) healAmount + " HP at the cost of " + (int) selfCost
                + " of your own health. The cost can never kill you.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Ally heal", (int) healAmount + " HP"),
                new AbilityStat("Self cost", (int) selfCost + " HP"),
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
