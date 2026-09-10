package org.latios.arenaBrawl.abilities.offensive;

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
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class Void implements Ability {

    private final AbilityCost cost;
    private final double initialDamage;
    private final double pullDamage;
    private final double pullRadius;
    private final double durationSeconds;
    private final double energyCost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Plugin plugin;

    public Void(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.initialDamage = config.getDouble("damage", 250.0);
        this.pullDamage = config.getDouble("pull-damage", 10.0);
        this.pullRadius = config.getDouble("pull-radius", 5.0);
        this.durationSeconds = config.getDouble("pull-duration-seconds", 2.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Void"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation();

        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);
        center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.5f);

        for (Player target : center.getWorld().getPlayers()) {
            if (target.equals(player) || !teamManager.isEnemy(player, target) || target.isDead()) {
                continue;
            }

            if (target.getLocation().distance(center) <= pullRadius) {
                combatService.applyAbilityDamage(player, target, initialDamage, getName(), target.getLocation());
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = (int) (durationSeconds * 20);

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                center.getWorld().spawnParticle(Particle.PORTAL, center, 25, 0.5, 0.5, 0.5, 0.5);
                center.getWorld().spawnParticle(Particle.SQUID_INK, center, 10, 0.3, 0.3, 0.3, 0.02);

                for (Player target : center.getWorld().getPlayers()) {
                    if (target.equals(player) || !teamManager.isEnemy(player, target) || target.isDead()) {
                        continue;
                    }

                    double distance = target.getLocation().distance(center);
                    if (distance <= pullRadius && distance > 0.5) {
                        Vector direction = center.toVector().subtract(target.getLocation().toVector()).normalize();
                        target.setVelocity(direction.multiply(0.35));

                        if (ticks % 20 == 0) {
                            combatService.applyAbilityDamage(player, target, pullDamage, getName(), target.getLocation());
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.6f, 0.8f);
                        }
                    }
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Creates a void vortex around you that deals initial damage and continuously pulls nearby enemies into its center while dealing extra damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Initial Damage", String.valueOf((int) initialDamage)),
                new AbilityStat("Pull Damage", (int) pullDamage + "/s"),
                new AbilityStat("Radius", (int) pullRadius + "m"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}