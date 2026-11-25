package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class Shackle implements Ability {

    private final Plugin plugin;
    private final double energyCost;
    private final double initialDamage;
    private final double damage;
    private final double maxRange;
    private final long durationMs;
    private final double breakDistance;
    private final long immobilizeDurationMs;
    private final int tickInterval;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public Shackle(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                   CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 45.0);
        this.initialDamage = config.getDouble("initial-damage", 40.0);
        this.damage = config.getDouble("damage", 180.0);
        this.maxRange = config.getDouble("max-range", 20.0);
        this.durationMs = config.getLong("duration-ms", 3500L);
        this.breakDistance = config.getDouble("break-distance", 3.0);
        this.immobilizeDurationMs = config.getLong("immobilize-duration-ms", 1200L);
        this.tickInterval = Math.max(1, config.getInt("tick-interval-ticks", 4));

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Shackle";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);
        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        Location anchor = target.getLocation().clone();
        long endAt = System.currentTimeMillis() + durationMs;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.2f, 1.2f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.8f);
        spawnAnchorRing(anchor);

        combatService.applyAbilityDamage(player, target, initialDamage, getName(), target.getLocation());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= endAt
                        || !player.isOnline() || !target.isOnline()
                        || player.isDead() || target.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || target.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)
                        || !config.getMatchManager().isInMatch(target)) {
                    cancel();
                    return;
                }

                Location current = target.getLocation();
                drawChain(anchor.clone().add(0, 0.2, 0), current.clone().add(0, 1.0, 0));

                double dx = current.getX() - anchor.getX();
                double dz = current.getZ() - anchor.getZ();
                if (dx * dx + dz * dz <= breakDistance * breakDistance) return;

                combatService.applyAbilityDamage(player, target, damage, getName(), current);
                debuffManager.tryApply(player, target, DebuffType.IMMOBILIZE, immobilizeDurationMs);

                target.getWorld().spawnParticle(Particle.CRIT, current.clone().add(0, 1.0, 0),
                        25, 0.35, 0.6, 0.35, 0.1);
                target.getWorld().playSound(current, Sound.BLOCK_CHAIN_BREAK, 1.4f, 1.0f);

                cancel();
            }
        }.runTaskTimer(plugin, 0L, tickInterval);

        return true;
    }

    private void spawnAnchorRing(Location anchor) {
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            Location point = anchor.clone().add(Math.cos(angle) * breakDistance, 0.1,
                    Math.sin(angle) * breakDistance);
            anchor.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }
    }

    private void drawChain(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length <= 0) return;

        direction.normalize().multiply(0.35);
        Location current = start.clone();
        double traveled = 0;

        while (traveled < length) {
            current.getWorld().spawnParticle(Particle.DUST, current, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(60, 60, 70), 0.8f));
            current.add(direction);
            traveled += 0.35;
        }
    }

    @Override
    public String getDescription() {
        return "Chains an enemy to the ground where they stand for light damage. If they move more than "
                + String.format("%.1f", breakDistance) + " blocks away, the chain snaps for heavy damage and immobilizes them.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Initial Damage", String.valueOf((int) initialDamage)),
                new AbilityStat("Break Damage", String.valueOf((int) damage)),
                new AbilityStat("Duration", (durationMs / 1000.0) + "s"),
                new AbilityStat("Leash Radius", String.format("%.1f blocks", breakDistance)),
                new AbilityStat("Immobilize", (immobilizeDurationMs / 1000.0) + "s"),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
