package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.AbilityCostReduction;
import org.latios.arenaBrawl.abilities.cost.OffensiveEnergyDiscount;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class Rampage implements Ability {

    private final double energyCost;
    private final double damage;
    private final double radius;
    private final double verticalRadius;
    private final double reductionPerHitPercent;
    private final double maxReductionPercent;
    private final int maxStacks;
    private final long reductionDurationMs;

    private final EnergyManager energyManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityCost cost;

    private int stacks;
    private long stacksExpireAt;
    private Double pendingCostMultiplier;

    public Rampage(EnergyManager energyManager, TeamManager teamManager,
                   CombatService combatService, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 40.0);
        this.damage = config.getDouble("damage", 120.0);
        this.radius = config.getDouble("radius", 4.5);
        this.verticalRadius = config.getDouble("vertical-radius", 3.0);
        this.reductionPerHitPercent = config.getDouble("reduction-per-hit-percent", 20.0);
        this.maxReductionPercent = config.getDouble("max-reduction-percent", 60.0);
        this.reductionDurationMs = config.getLong("reduction-duration-ms", 6000L);

        this.maxStacks = reductionPerHitPercent > 0
                ? Math.max(1, (int) Math.floor(maxReductionPercent / reductionPerHitPercent))
                : 0;

        this.energyManager = energyManager;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.cost = new RampageCost();
    }

    private final class RampageCost implements AbilityCost {

        @Override
        public boolean canPay(Player player) {
            return canPay(player, null);
        }

        @Override
        public boolean canPay(Player player, AbilitySlot slot) {
            double multiplier = 1.0 - currentReduction();
            return energyManager.hasEnough(player, effectiveCost(player, slot, multiplier));
        }

        @Override
        public void pay(Player player) {
            pay(player, null);
        }

        @Override
        public void pay(Player player, AbilitySlot slot) {
            double multiplier = pendingCostMultiplier != null
                    ? pendingCostMultiplier
                    : (1.0 - currentReduction());
            pendingCostMultiplier = null;

            double finalCost = effectiveCost(player, slot, multiplier);
            if (finalCost > 0.0) {
                energyManager.consume(player, finalCost);
            }
        }

        @Override
        public String describeRemaining(Player player) {
            return "You don't have enough energy";
        }

        @Override
        public boolean usesEnergy() {
            return true;
        }

        @Override
        public String getBaseCostDescription() {
            return (int) energyCost + " energy";
        }
    }

    private double effectiveCost(Player player, AbilitySlot slot, double rampageMultiplier) {
        if (rampageMultiplier <= 0.0) {
            return 0.0;
        }
        double categoryMultiplier = slot != null ? AbilityCostReduction.getMultiplier(player, slot) : 1.0;
        return energyCost * rampageMultiplier * OffensiveEnergyDiscount.getMultiplier(player) * categoryMultiplier;
    }

    private double currentReduction() {
        if (stacks <= 0) {
            return 0.0;
        }
        long now = System.currentTimeMillis();
        if (now >= stacksExpireAt) {
            stacks = 0;
            return 0.0;
        }
        double reduction = stacks * (reductionPerHitPercent / 100.0);
        return Math.min(maxReductionPercent / 100.0, reduction);
    }

    @Override
    public String getName() {
        return "Rampage";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        double currentMultiplier = 1.0 - currentReduction();

        int hits = 0;
        for (Entity nearby : player.getNearbyEntities(radius, verticalRadius, radius)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.equals(player) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, target)) continue;

            combatService.applyAbilityDamage(player, target, damage, getName(), player.getLocation());
            hits++;
        }

        playImpact(player);

        pendingCostMultiplier = currentMultiplier;

        if (hits > 0) {
            stacks = Math.min(maxStacks, stacks + 1);
            stacksExpireAt = System.currentTimeMillis() + reductionDurationMs;
        }

        return true;
    }

    private void playImpact(Player player) {
        Location center = player.getLocation();
        int points = 28;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);
            point.getWorld().spawnParticle(Particle.SOUL, point, 1, 0.05, 0.05, 0.05, 0.01);
        }

        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1.0, 0),
                25, radius * 0.4, 0.5, radius * 0.4, 0.03);
        center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 1.0, 0),
                4, radius * 0.35, 0.2, radius * 0.35, 0.0);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 10.0f, 0.75f);
        center.getWorld().playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 10.0f, 1.3f);
        center.getWorld().playSound(center, Sound.ENTITY_VEX_CHARGE, 10.0f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 10.0f, 1.4f);
    }

    @Override
    public String getDescription() {
        return "Strikes every nearby enemy. Each cast that hits at least one enemy reduces "
                + "Rampage's energy cost by " + (int) reductionPerHitPercent + "% for "
                + (reductionDurationMs / 1000L) + "s (refreshing on hit), stacking up to "
                + (int) maxReductionPercent + "%.";
    }
    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Radius", String.format("%.1f blocks", radius)),
                new AbilityStat("Reduction per Hit", (int) reductionPerHitPercent + "%"),
                new AbilityStat("Max Reduction", (int) maxReductionPercent + "%"),
                new AbilityStat("Reduction Duration", (reductionDurationMs / 1000L) + "s"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}