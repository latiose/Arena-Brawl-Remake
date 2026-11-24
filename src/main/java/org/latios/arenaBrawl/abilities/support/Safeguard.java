package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Safeguard implements Ability {

    private static final double TRAIL_STEP = 0.35;

    private final long cooldownMs;
    private final double healAmount;
    private final double selfHeal;
    private final double maxRange;
    private final double stopDistance;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public Safeguard(CooldownManager cooldownManager, TeamManager teamManager,
                     PlayerHealthManager healthManager, CombatUpgradeManager combatUpgradeManager,
                     AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 12000L);
        this.healAmount = config.getDouble("heal-amount", 150.0);
        this.selfHeal = config.getDouble("self-heal", 75.0);
        this.maxRange = config.getDouble("max-range", 12.0);
        this.stopDistance = config.getDouble("stop-distance", 1.2);

        this.cost = new CooldownCost(cooldownManager, "safeguard", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() {
        return "Safeguard";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player ally = AbilityTargeting.findAllyAlongRay(player, teamManager, maxRange);

        if (ally == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        Location start = player.getLocation().clone();
        Location landing = resolveLanding(player, ally);

        player.teleport(landing);
        drawTrail(start, landing);

        player.getWorld().playSound(landing, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.5f);
        player.getWorld().playSound(landing, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
        ally.getWorld().playSound(ally.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);

        if (selfHeal > 0) {
            healthManager.heal(player, selfHeal, getName());
        }
        healthManager.healAlly(player, ally, healAmount, getName());

        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 1.6, 0), 8, 0.3, 0.3, 0.3, 0.02);
        ally.getWorld().spawnParticle(Particle.FIREWORK, ally.getLocation().add(0, 1.0, 0), 18, 0.3, 0.4, 0.3, 0.05);

        return true;
    }

    private Location resolveLanding(Player caster, Player ally) {
        Location targetLoc = ally.getLocation().clone();

        Vector away = caster.getLocation().toVector().subtract(targetLoc.toVector());
        away.setY(0);
        if (away.lengthSquared() < 1e-6) {
            away = targetLoc.getDirection().setY(0);
        }
        away.normalize().multiply(stopDistance);

        Location landing = targetLoc.clone().add(away);
        landing.setY(targetLoc.getY());

        if (isBlocked(landing)) {
            landing = AbilityTargeting.findBehindLocation(ally);
        }

        landing.setDirection(caster.getLocation().getDirection());
        return landing;
    }

    private boolean isBlocked(Location location) {
        return location.getBlock().getType().isSolid()
                || location.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private void drawTrail(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length <= 0) return;

        direction.normalize().multiply(TRAIL_STEP);
        Location point = from.clone().add(0, 1.0, 0);
        double traveled = 0;

        while (traveled < length) {
            point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0.01);
            point.getWorld().spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05,
                    new Particle.DustOptions(Color.fromRGB(255, 245, 200), 1.0f));
            point.add(direction);
            traveled += TRAIL_STEP;
        }
    }

    @Override
    public String getDescription() {
        return "Dashes to a targeted ally and heals them for " + (int) healAmount
                + " HP, also healing you for " + (int) selfHeal + " HP. Can only target allies.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Ally Heal", (int) healAmount + " HP"),
                new AbilityStat("Self Heal", (int) selfHeal + " HP"),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Target", "Ally only"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
