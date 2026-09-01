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

public class SpikeGrenade implements Ability {

    private final double energyCost;
    private final double grenadeDamage;
    private final double needleDamage;
    private final double maxRange;
    private final double step;
    private final int needleCount;
    private final double needleRange;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public SpikeGrenade(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                        CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 30.0);
        this.grenadeDamage = config.getDouble("grenade-damage", 60.0);
        this.needleDamage = config.getDouble("needle-damage", 5.0);
        this.maxRange = config.getDouble("max-range", 15.0);
        this.step = config.getDouble("step", 0.5);
        this.needleCount = config.getInt("needle-count", 6);
        this.needleRange = config.getDouble("needle-range", 6.0);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Spike Grenade";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        player.getWorld().playSound(startLoc, Sound.ENTITY_EGG_THROW, 1.2f, 0.7f);

        new BukkitRunnable() {
            private Location currentLoc = startLoc.clone();
            private double distanceTraveled = 0;

            @Override
            public void run() {
                currentLoc.add(direction.clone().multiply(step));
                distanceTraveled += step;

                currentLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, currentLoc, 8, 0.2, 0.2, 0.2, 0.05);
                currentLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, currentLoc, 3, 0.15, 0.15, 0.15, 0);
                currentLoc.getWorld().spawnParticle(Particle.SCRAPE, currentLoc, 2, 0.1, 0.1, 0.1, 0.01);

                if (currentLoc.getBlock().getType().isSolid()) {
                    explode(player, currentLoc);
                    cancel();
                    return;
                }

                for (Player enemy : currentLoc.getWorld().getPlayers()) {
                    if (!teamManager.isEnemy(player, enemy) || enemy.isDead()) continue;

                    if (enemy.getBoundingBox().expand(0.3, 0.3, 0.3).contains(currentLoc.getX(), currentLoc.getY(), currentLoc.getZ())) {
                        combatService.applyAbilityDamage(player, enemy, grenadeDamage, getName(), currentLoc);
                        explode(player, currentLoc);
                        cancel();
                        return;
                    }
                }

                if (distanceTraveled >= maxRange) {
                    explode(player, currentLoc);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void explode(Player caster, Location center) {
        center.getWorld().playSound(center, Sound.ENTITY_ITEM_BREAK, 1.4f, 0.5f);
        center.getWorld().playSound(center, Sound.BLOCK_BONE_BLOCK_BREAK, 1.2f, 1.4f);

        center.getWorld().spawnParticle(Particle.ITEM_SLIME, center, 35, 0.4, 0.4, 0.4, 0.15);
        center.getWorld().spawnParticle(Particle.SCRAPE, center, 20, 0.3, 0.3, 0.3, 0.1);

        double angleStep = 360.0 / needleCount;
        for (int i = 0; i < needleCount; i++) {
            double radians = Math.toRadians(i * angleStep);
            Vector needleDir = new Vector(Math.cos(radians), 0, Math.sin(radians)).normalize();
            launchNeedle(caster, center.clone(), needleDir);
        }
    }

    private void launchNeedle(Player caster, Location start, Vector dir) {
        new BukkitRunnable() {
            private Location needleLoc = start.clone();
            private double dist = 0;

            @Override
            public void run() {
                needleLoc.add(dir.clone().multiply(step));
                dist += step;

                needleLoc.getWorld().spawnParticle(Particle.CRIT, needleLoc, 2, 0.05, 0.05, 0.05, 0.02);
                needleLoc.getWorld().spawnParticle(Particle.SCRAPE, needleLoc, 2, 0.05, 0.05, 0.05, 0.01);

                if (needleLoc.getBlock().getType().isSolid()) {
                    needleLoc.getWorld().spawnParticle(Particle.CRIT, needleLoc, 5, 0.1, 0.1, 0.1, 0.05);
                    cancel();
                    return;
                }

                for (Player enemy : needleLoc.getWorld().getPlayers()) {
                    if (!teamManager.isEnemy(caster, enemy) || enemy.isDead()) continue;

                    if (enemy.getBoundingBox().expand(0.3, 0.3, 0.3).contains(needleLoc.getX(), needleLoc.getY(), needleLoc.getZ())) {
                        combatService.applyAbilityDamage(caster, enemy, needleDamage, getName(), needleLoc);
                        enemy.getWorld().playSound(needleLoc, Sound.ENTITY_PLAYER_HURT, 0.8f, 1.8f);
                        enemy.getWorld().spawnParticle(Particle.CRIT, needleLoc, 8, 0.2, 0.2, 0.2, 0.1);
                        cancel();
                        return;
                    }
                }

                if (dist >= needleRange) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public String getDescription() {
        return "Fires a cactus grenade that explodes on contact or max range, splitting into 6 needles in all directions.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Direct Damage", String.valueOf((int) grenadeDamage)),
                new AbilityStat("Needle Damage", String.valueOf((int) needleDamage)),
                new AbilityStat("Energy", String.valueOf((int) energyCost))
        );
    }
}