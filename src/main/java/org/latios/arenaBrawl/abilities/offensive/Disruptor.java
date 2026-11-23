package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class Disruptor implements Ability {

    private final Plugin plugin;
    private final double energyCost;
    private final double damage;
    private final long silenceDurationMs;
    private final double radius;
    private final double maxRange;
    private final int travelTicks;
    private final int armTicks;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public Disruptor(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                     CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 60.0);
        this.damage = config.getDouble("damage", 70.0);
        this.silenceDurationMs = config.getLong("silence-duration-ms", 1500L);
        this.radius = config.getDouble("radius", 3.5);
        this.maxRange = config.getDouble("max-range", 18.0);
        this.travelTicks = Math.max(1, config.getInt("travel-ticks", 8));
        this.armTicks = Math.max(0, (int) (config.getLong("arm-ms", 500L) / 50L));

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Disruptor";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location landing = resolveLanding(player);
        Location start = player.getEyeLocation().clone();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.2f, 1.4f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)) {
                    cancel();
                    return;
                }

                if (tick < travelTicks) {
                    double progress = (double) tick / travelTicks;
                    Location point = start.clone().add(
                            landing.toVector().subtract(start.toVector()).multiply(progress));
                    point.add(0, Math.sin(Math.PI * progress) * 1.5, 0);

                    point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 4, 0.05, 0.05, 0.05, 0.02);
                    point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
                    tick++;
                    return;
                }

                if (tick == travelTicks) {
                    landing.getWorld().playSound(landing, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
                }

                if (tick < travelTicks + armTicks) {
                    spawnArmRing(landing, tick - travelTicks);
                    tick++;
                    return;
                }

                detonate(player, landing);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Location resolveLanding(Player player) {
        RayTraceResult result = player.rayTraceBlocks(maxRange);
        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition().toLocation(player.getWorld());
        }

        Vector direction = player.getEyeLocation().getDirection().normalize();
        return player.getEyeLocation().clone().add(direction.multiply(maxRange));
    }

    private void spawnArmRing(Location center, int step) {
        int points = 16;
        double pulse = 0.6 + (step % 5) * 0.12;

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            Location point = center.clone().add(
                    Math.cos(angle) * radius * pulse, 0.1,
                    Math.sin(angle) * radius * pulse);
            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0.01);
        }
    }

    private void detonate(Player caster, Location center) {
        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 45,
                radius * 0.35, 0.4, radius * 0.35, 0.06);
        center.getWorld().spawnParticle(Particle.ENCHANT, center, 25, radius * 0.3, 0.5, radius * 0.3, 0.4);
        center.getWorld().playSound(center, Sound.ENTITY_SHULKER_BULLET_HIT, 1.6f, 1.1f);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.4f, 0.7f);

        for (Player target : center.getWorld().getPlayers()) {
            if (target.isDead() || target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (distanceToBox(center, target.getBoundingBox()) > radius) continue;

            debuffManager.tryApply(caster, target, DebuffType.SILENCE, silenceDurationMs);
            target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 2.0, 0),
                    20, 0.35, 0.3, 0.35, 0.45);
            combatService.applyAbilityDamage(caster, target, damage, getName(), center);
        }
    }

    private double distanceToBox(Location center, BoundingBox box) {
        double x = Math.max(box.getMinX(), Math.min(center.getX(), box.getMaxX()));
        double y = Math.max(box.getMinY(), Math.min(center.getY(), box.getMaxY()));
        double z = Math.max(box.getMinZ(), Math.min(center.getZ(), box.getMaxZ()));
        return center.toVector().distance(new Vector(x, y, z));
    }

    @Override
    public String getDescription() {
        return "Lobs a disruption grenade that arms where it lands, then detonates to deal light area damage and silence every enemy caught in the blast.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Silence", (silenceDurationMs / 1000.0) + "s"),
                new AbilityStat("Blast Radius", String.format("%.1f blocks", radius)),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Arm Time", (armTicks * 50L) + "ms"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
