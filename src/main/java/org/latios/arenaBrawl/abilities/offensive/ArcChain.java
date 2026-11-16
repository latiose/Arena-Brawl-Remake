package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArcChain implements Ability {

    private final double energyCost;
    private final double primaryDamage;
    private final double secondaryDamage;
    private final double tertiaryDamage;
    private final double maxRange;
    private final double chainRadius;
    private final int maxJumps;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public ArcChain(EnergyManager energyManager, TeamManager teamManager,
                    CombatService combatService, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 55.0);
        this.primaryDamage = config.getDouble("damage", 100.0);
        this.secondaryDamage = config.getDouble("secondary-damage", 70.0);
        this.tertiaryDamage = config.getDouble("tertiary-damage", 45.0);
        this.maxRange = config.getDouble("max-range", 22.0);
        this.chainRadius = config.getDouble("chain-radius", 6.0);
        this.maxJumps = config.getInt("max-jumps", 2);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Arc Chain";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Launches a bolt of lightning at an enemy that arcs to nearby enemies, dealing less damage with each jump.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) primaryDamage + " / " + (int) secondaryDamage + " / " + (int) tertiaryDamage),
                new AbilityStat("Jumps", String.valueOf(maxJumps)),
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }

    @Override
    public boolean activate(Player player) {
        Player first = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (first == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        Set<Player> hit = new HashSet<>();
        hit.add(player);

        List<Player> chain = new ArrayList<>();
        Player current = first;
        chain.add(current);
        hit.add(current);

        for (int jump = 0; jump < maxJumps; jump++) {
            Player next;
            if (jump == 1 && chain.size() > 1) {
                next = first;
            } else {
                next = findNearestEnemy(player, current, hit);
            }
            if (next == null) break;
            chain.add(next);
            hit.add(next);
            current = next;
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10.0f, 1.6f);

        Location origin = player.getEyeLocation();
        Location previous = origin;

        for (int index = 0; index < chain.size(); index++) {
            Player target = chain.get(index);
            Location targetPoint = target.getLocation().add(0, 1.0, 0);

            drawArc(previous, targetPoint);
            previous = targetPoint;

            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetPoint, 20, 0.3, 0.5, 0.3, 0.1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 10.0f, 1.4f);

            combatService.applyAbilityDamage(player, target, damageFor(index), getName(), targetPoint);
        }

        return true;
    }

    private double damageFor(int index) {
        if (index == 0) return primaryDamage;
        if (index == 1) return secondaryDamage;
        return tertiaryDamage;
    }

    private Player findNearestEnemy(Player caster, Player from, Set<Player> excluded) {
        Player best = null;
        double bestDistance = chainRadius * chainRadius;

        for (Player candidate : from.getWorld().getPlayers()) {
            if (candidate.equals(caster) || excluded.contains(candidate)) continue;
            if (candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, candidate)) continue;

            double distance = candidate.getLocation().distanceSquared(from.getLocation());
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }

    private void drawArc(Location from, Location to) {
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        if (length < 1e-4) return;

        int steps = Math.max(2, (int) (length / 0.25));

        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            Location point = from.clone().add(delta.clone().multiply(progress));
            double offset = Math.sin(progress * Math.PI) * 0.35;
            point.add((Math.random() - 0.5) * offset, (Math.random() - 0.5) * offset, (Math.random() - 0.5) * offset);
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0.02);
        }
    }
}
