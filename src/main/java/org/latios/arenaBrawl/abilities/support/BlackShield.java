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
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * A dark shield for an ally (or the caster when no ally is aimed at) that
 * reduces incoming damage and blocks all debuffs while it lasts.
 */
public class BlackShield implements Ability {

    private final long cooldownMs;
    private final double reductionPercent;
    private final long durationMs;
    private final long immunityMs;
    private final double healAmount;
    private final double maxRange;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public BlackShield(CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                       TeamManager teamManager, ShieldManager shieldManager, DebuffManager debuffManager,
                       PlayerHealthManager healthManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30_000L);
        this.reductionPercent = Math.max(0.0, Math.min(1.0, config.getDouble("damage-reduction", 0.50)));
        this.durationMs = config.getLong("duration-ms", 4_000L);
        this.immunityMs = config.getLong("debuff-immunity-ms", durationMs);
        this.healAmount = Math.max(0.0, config.getDouble("heal-amount", 100.0));
        this.maxRange = config.getDouble("max-range", 12.0);

        this.cost = new CooldownCost(cooldownManager, "blackshield", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Black Shield";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player target = findAlly(player);
        if (target == null) target = player;

        shieldManager.applySilentPulsingShield(target, reductionPercent, durationMs, "Black Shield");
        debuffManager.grantDebuffImmunity(target, immunityMs);

        healthManager.healAlly(player, target, healAmount,getName());


        Location center = target.getLocation().add(0, 1.0, 0);
        center.getWorld().spawnParticle(Particle.HEART, center, 8, 0.3, 0.4, 0.3, 0.0);
        center.getWorld().spawnParticle(Particle.SQUID_INK, center, 25, 0.45, 0.6, 0.45, 0.02);
        center.getWorld().spawnParticle(Particle.SMOKE, center, 20, 0.4, 0.5, 0.4, 0.01);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.7f);

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
        return null;
    }

    @Override
    public String getDescription() {
        return "Shields an ally (or yourself if you aim at nobody), instantly healing them, reducing damage by "
                + (int) (reductionPercent * 100) + "% and blocking all debuffs while it lasts.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Instant heal", (int) healAmount + " HP"),
                new AbilityStat("Damage reduction", (int) (reductionPercent * 100) + "%"),
                new AbilityStat("Duration", (durationMs / 1000.0) + "s"),
                new AbilityStat("Debuff immunity", (immunityMs / 1000.0) + "s"),
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
