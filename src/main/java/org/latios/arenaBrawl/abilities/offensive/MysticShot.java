package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class MysticShot implements Ability {

    private final double damage;
    private final double energyCost;
    private final double maxDistance;
    private final double step;
    private final double hitBoxRadius;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public MysticShot(EnergyManager energyManager, TeamManager teamManager,
                      CombatService combatService, AbilityConfig config) {
        this.damage = config.getDouble("damage", 185.0);
        this.energyCost = config.getDouble("energy-cost", 70.0);
        this.maxDistance = config.getDouble("max-distance", 25.0);
        this.step = config.getDouble("step", 1.3);
        this.hitBoxRadius = config.getDouble("hit-box-radius", 1.0);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Mystic Shot";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Fires a fast bolt of energy in a line, dealing damage to the first enemy hit.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Range", (int) maxDistance + " blocks")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = startLoc.getDirection().normalize();

        player.getWorld().playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1.0f, 1.6f);

        new BukkitRunnable() {
            private Location currentLoc = startLoc.clone();
            private double distanceTraveled = 0.0;

            @Override
            public void run() {
                currentLoc.add(direction.clone().multiply(step));
                distanceTraveled += step;

                currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 3, 0.05, 0.05, 0.05, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 1, 0, 0, 0, 0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 10, 0.2, 0.2, 0.2, 0.1);
                    currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
                    cancel();
                    return;
                }

                for (Player target : currentLoc.getWorld().getPlayers()) {
                    if (target.equals(player) || target.isDead()) continue;
                    if (!teamManager.isEnemy(player, target)) continue;

                    if (target.getLocation().add(0, 1.0, 0).distance(currentLoc) <= hitBoxRadius) {
                        combatService.applyAbilityDamage(player, target, damage, getName());

                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
                        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1.0, 0), 15, 0.3, 0.4, 0.3, 0.1);

                        cancel();
                        return;
                    }
                }

                if (distanceTraveled >= maxDistance) {
                    cancel();
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }
}