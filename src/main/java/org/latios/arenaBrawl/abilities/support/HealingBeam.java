package org.latios.arenaBrawl.abilities.support;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HealingBeam implements Ability {

    private final long cooldownMs;
    private final double healAmount;
    private final double maxRange;
    private final double stepSize;
    private final double hitExpansion;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;


    public HealingBeam(CooldownManager cooldownManager, TeamManager teamManager,
                       PlayerHealthManager healthManager,
                       CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 10000L);
        this.healAmount = config.getDouble("heal-amount", 80.0);
        this.maxRange = config.getDouble("max-range", 256.0);
        this.stepSize = config.getDouble("step-size", 0.4);
        this.hitExpansion = config.getDouble("hit-expansion", 0.6);

        this.cost = new CooldownCost(cooldownManager, "healingbeam", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Healing Beam"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Fires a ray of light that heals you and any allies it passes through";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", (int) healAmount + " HP"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Range", "Infinite")
        );
    }

    @Override
    public boolean activate(Player player) {
        healthManager.heal(player, healAmount, getName());
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 20, 0.2, 0.4, 0.2, 0.05);

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Set<Player> hitAllies = new HashSet<>();
        Set<Player> potentialAllies = new HashSet<>();

        for (Entity entity : player.getWorld().getNearbyEntities(eye, maxRange, maxRange, maxRange)) {
            if (entity instanceof Player candidate
                    && !candidate.equals(player)
                    && candidate.getGameMode() != GameMode.SPECTATOR
                    && teamManager.isAlly(player, candidate)) {
                potentialAllies.add(candidate);
            }
        }

        for (double distance = stepSize; distance <= maxRange; distance += stepSize) {
            Location point = eye.clone().add(direction.clone().multiply(distance));
            if (point.getBlock().getType().isSolid()) {
                break;
            }
            point.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, point, 1, 0, 0, 0, 0);

            Vector pointVector = point.toVector();
            for (Player candidate : potentialAllies) {
                BoundingBox box = candidate.getBoundingBox().expand(hitExpansion);
                if (box.contains(pointVector)) {
                    hitAllies.add(candidate);
                }
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.5f);

        for (Player ally : hitAllies) {
            healthManager.healAlly(player, ally, healAmount, getName());
            ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.1);
            ally.getWorld().playSound(ally.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }

        return true;
    }
}