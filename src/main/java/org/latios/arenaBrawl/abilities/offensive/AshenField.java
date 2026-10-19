package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Offensive area denial. Ignites a burning field on a targeted block that damages any enemy
 * standing inside it over time. Useful to contest the central power-ups in a 2v2.
 */
public class AshenField implements Ability {

    private final double energyCost;
    private final double damagePerPulse;
    private final double maxTargetDistance;
    private final double radius;
    private final long durationTicks;
    private final long pulseIntervalTicks;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityConfig config;

    public AshenField(EnergyManager energyManager, TeamManager teamManager,
                      CombatService combatService, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 60.0);
        this.damagePerPulse = config.getDouble("damage-per-pulse", 30.0);
        this.maxTargetDistance = config.getDouble("max-target-distance", 20.0);
        this.radius = config.getDouble("radius", 4.0);
        this.durationTicks = config.getLong("duration-ticks", 100L);
        this.pulseIntervalTicks = Math.max(1L, config.getLong("pulse-interval-ticks", 20L));

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Ashen Field";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Ignites a burning field where you aim. Enemies standing inside take damage every second.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage per Second", (int) damagePerPulse + ""),
                new AbilityStat("Duration", (durationTicks * 0.05) + "s"),
                new AbilityStat("Radius", radius + " blocks"),
                new AbilityStat("Energy Cost", (int) energyCost + "")
        );
    }

    @Override
    public boolean activate(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxTargetDistance,
                FluidCollisionMode.NEVER,
                true
        );

        if (result == null || result.getHitBlock() == null) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        if (result.getHitBlockFace() != null && result.getHitBlockFace().getModY() < 0) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        Location center = result.getHitPosition().toLocation(player.getWorld());

        MatchSoundUtils.play(config, player, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed++;

                if (ticksElapsed > durationTicks) {
                    cancel();
                    return;
                }

                drawField(center);

                if (ticksElapsed % pulseIntervalTicks == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.4f);

                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius + 1.5, radius)) {
                        if (!(entity instanceof Player victim)) continue;
                        if (victim.equals(player) || victim.isDead()) continue;
                        if (!teamManager.isEnemy(player, victim)) continue;
                        if (victim.getLocation().distanceSquared(center) > radius * radius) continue;

                        combatService.applyAbilityDamage(player, victim, damagePerPulse, getName(), center);
                    }
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    private void drawField(Location center) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location edge = center.clone().add(x, 0.1, z);
            edge.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, edge, 1, 0, 0, 0, 0.005);
        }

        center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 0.3, 0), 12,
                radius * 0.6, 0.3, radius * 0.6, 0.01);
        center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, 0.5, 0), 6,
                radius * 0.5, 0.4, radius * 0.5, 0.01);
        center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(0, 0.2, 0), 2,
                radius * 0.5, 0.1, radius * 0.5, 0.0);
    }
}
