package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlameBreath extends Breath {

    private final double damage;
    private final double tickDamage;
    private final double energyCost;

    public FlameBreath(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                       CombatService combatService, AbilityConfig config) {
        super(plugin, energyManager, config.getDouble("energy-cost", 60.0), teamManager, combatService, null);
        this.damage = config.getDouble("damage", 150.0);
        this.tickDamage = config.getDouble("tick-damage", 25.0);
        this.energyCost = config.getDouble("energy-cost", 60.0);
    }

    @Override
    public String getName() { return "Flame Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_ENDER_DRAGON_GROWL;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.FLAME, point, 1, 0, 0, 0, 0);
    }

    @Override
    protected void applyHitEffects(Player target) {

    }

    @Override
    public boolean activate(Player player) {
        player.getWorld().playSound(player.getLocation(), getCastSound(), 1.0f, 1.0f);
        Location origin = player.getLocation().add(0, 1.4, 0);

        Vector axis = player.getEyeLocation().getDirection().setY(0);
        if (axis.lengthSquared() < 1e-6) {
            axis = player.getLocation().getDirection().setY(0);
        }
        axis.normalize();

        Vector right = new Vector(-axis.getZ(), 0, axis.getX()).normalize();
        Vector up = new Vector(0, 1, 0);

        List<List<Location>> slices = new ArrayList<>();
        List<Location> allConePoints = new ArrayList<>();
        Set<Player> initialHitPlayers = new HashSet<>();

        double currentAngle = 0.0;
        for (double distance = -1.0; distance <= getMaxParticleDistance(); distance += STEP_SIZE) {
            double radius = Math.abs(distance) * RADIUS_GROWTH;
            double deltaTheta = 0.35 + (0.05 / (Math.abs(distance) + 0.1));
            currentAngle += deltaTheta;

            Location center = origin.clone().add(axis.clone().multiply(distance));

            Location strand1 = helixPoint(center, right, up, radius, currentAngle);
            Location strand2 = helixPoint(center, right, up, radius, currentAngle + Math.PI);

            List<Location> slice = List.of(strand1, strand2);
            slices.add(slice);
            allConePoints.addAll(slice);

            checkHit(player, strand1, initialHitPlayers);
            checkHit(player, strand2, initialHitPlayers);
        }

        for (Player target : initialHitPlayers) {
            combatService.applyAbilityDamage(player, target, getDamage(), getName());
            applySpike(target);
        }

        int totalSlices = slices.size();

        new BukkitRunnable() {
            int tick = 0;
            int lastSpawnedSliceIndex = 0;

            @Override
            public void run() {
                tick++;

                if (tick <= 20) {
                    int maxSliceToSpawn = (int) Math.ceil((double) tick / 20.0 * totalSlices);
                    int targetIndex = Math.min(maxSliceToSpawn, totalSlices);

                    for (int i = lastSpawnedSliceIndex; i < targetIndex; i++) {
                        for (Location pt : slices.get(i)) {
                            spawnTrailParticles(pt);
                        }
                    }
                    lastSpawnedSliceIndex = targetIndex;
                } else {
                    for (Location pt : allConePoints) {
                        pt.getWorld().spawnParticle(Particle.FLAME, pt, 1, 0.05, 0.05, 0.05, 0);
                    }
                }

                if (tick == 20 || tick == 40) {
                    Set<Player> tickHitPlayers = new HashSet<>();
                    for (Location pt : allConePoints) {
                        checkHit(player, pt, tickHitPlayers);
                    }

                    for (Player target : tickHitPlayers) {
                        combatService.applyAbilityDamage(player, target, tickDamage, getName());
                    }
                }

                if (tick >= 40) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage over the next 2 seconds to enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Tick damage", String.valueOf((int) tickDamage)),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", "8")
        );
    }
}