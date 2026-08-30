package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class SpikeGrenade implements Ability {

    private static final int ENERGY_COST = 30;
    private static final double GRENADE_DAMAGE = 60.0;
    private static final double NEEDLE_DAMAGE = 5.0;
    private static final double MAX_RANGE = 15.0;
    private static final double STEP = 0.5;
    private static final int NEEDLE_COUNT = 6;
    private static final double NEEDLE_RANGE = 6.0;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public SpikeGrenade(Plugin plugin, EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.plugin = plugin;
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
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
                currentLoc.add(direction.clone().multiply(STEP));
                distanceTraveled += STEP;

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
                        combatService.applyAbilityDamage(player, enemy, GRENADE_DAMAGE, getName(), currentLoc);
                        explode(player, currentLoc);
                        cancel();
                        return;
                    }
                }

                if (distanceTraveled >= MAX_RANGE) {
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

        double angleStep = 360.0 / NEEDLE_COUNT;
        for (int i = 0; i < NEEDLE_COUNT; i++) {
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
                needleLoc.add(dir.clone().multiply(STEP));
                dist += STEP;

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
                        combatService.applyAbilityDamage(caster, enemy, NEEDLE_DAMAGE, getName(), needleLoc);
                        enemy.getWorld().playSound(needleLoc, Sound.ENTITY_PLAYER_HURT, 0.8f, 1.8f);
                        enemy.getWorld().spawnParticle(Particle.CRIT, needleLoc, 8, 0.2, 0.2, 0.2, 0.1);
                        cancel();
                        return;
                    }
                }

                if (dist >= NEEDLE_RANGE) {
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
                new AbilityStat("Direct Damage", String.valueOf((int) GRENADE_DAMAGE)),
                new AbilityStat("Needle Damage", String.valueOf((int) NEEDLE_DAMAGE)),
                new AbilityStat("Energy", String.valueOf(ENERGY_COST))
        );
    }
}