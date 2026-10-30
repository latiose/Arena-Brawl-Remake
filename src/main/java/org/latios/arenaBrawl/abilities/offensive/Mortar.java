package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
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

/**
 * Arcing explosive shell. It is fired in the direction the player is looking,
 * then drops with gravity. Damage scales with how far the shell fell before
 * impact, rewarding shots fired from high ground.
 */
public class Mortar implements Ability {

    private static final double TRAIL_STEP = 0.5;

    private final double energyCost;
    private final double baseDamage;
    private final double damagePerFiveBlocks;
    private final double maxDamage;
    private final double aoeRadius;
    private final double projectileSpeed;
    private final double gravity;
    private final int maxFlightTicks;
    private final double hitRadius;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public Mortar(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                  CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.baseDamage = config.getDouble("base-damage", 90.0);
        this.damagePerFiveBlocks = config.getDouble("damage-per-5-blocks", 20.0);
        this.maxDamage = config.getDouble("max-damage", 200.0);
        this.aoeRadius = config.getDouble("aoe-radius", 3.5);
        this.projectileSpeed = config.getDouble("projectile-speed", 1.1);
        this.gravity = config.getDouble("gravity", 0.05);
        this.maxFlightTicks = config.getInt("max-flight-ticks", 100);
        this.hitRadius = config.getDouble("hit-radius", 0.5);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Mortar";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();
        if (direction.lengthSquared() < 0.0001) return false;
        direction.normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);

        new BukkitRunnable() {
            private final Location position = start.clone();
            private final Vector velocity = direction.clone().multiply(projectileSpeed);
            private final double startY = start.getY();
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                if (ticks++ >= maxFlightTicks) {
                    explode(player, position, startY);
                    cancel();
                    return;
                }

                velocity.setY(velocity.getY() - gravity);

                Vector delta = velocity.clone();
                double length = delta.length();
                if (length < 0.0001) {
                    cancel();
                    return;
                }

                Vector step = delta.clone().normalize();
                World world = position.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                RayTraceResult hit = world.rayTrace(
                        position, step, length,
                        FluidCollisionMode.NEVER,
                        true,
                        hitRadius,
                        entity -> entity instanceof Player target
                                && !target.equals(player)
                                && !target.isDead()
                                && target.getGameMode() != GameMode.SPECTATOR
                                && teamManager.isEnemy(player, target)
                );

                if (hit != null) {
                    Location impact = hit.getHitPosition().toLocation(world);
                    if (hit.getHitEntity() instanceof Player target) {
                        impact = target.getLocation().add(0, 1.0, 0);
                    }
                    explode(player, impact, startY);
                    cancel();
                    return;
                }

                spawnTrail(world, position, step, length);
                position.add(delta);

                if (position.getY() < world.getMinHeight() + 1) {
                    explode(player, position, startY);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void explode(Player caster, Location center, double startY) {
        World world = center.getWorld();
        if (world == null) return;

        double fall = Math.max(0.0, startY - center.getY());
        double damage = Math.min(maxDamage,
                baseDamage + (fall / 5.0) * damagePerFiveBlocks);

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f);
        world.spawnParticle(Particle.EXPLOSION, center, 2, 0.3, 0.2, 0.3, 0.0);
        world.spawnParticle(Particle.LAVA, center, 12, aoeRadius * 0.4, 0.3, aoeRadius * 0.4, 0.05);
        world.spawnParticle(Particle.SMOKE, center, 30, aoeRadius * 0.4, 0.4, aoeRadius * 0.4, 0.05);

        double radiusSq = aoeRadius * aoeRadius;
        for (Entity nearby : world.getNearbyEntities(center, aoeRadius, aoeRadius, aoeRadius)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.equals(caster) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (target.getLocation().add(0, 1.0, 0).distanceSquared(center) > radiusSq) continue;

            combatService.applyAbilityDamage(caster, target, damage, getName(), center);
        }
    }

    private void spawnTrail(World world, Location from, Vector direction, double length) {
        for (double distance = 0; distance < length; distance += TRAIL_STEP) {
            Location point = from.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.SMOKE, point, 2, 0.05, 0.05, 0.05, 0.01);
            world.spawnParticle(Particle.FLAME, point, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    @Override
    public String getDescription() {
        return "Launches an arcing explosive shell. It explodes on impact and deals more damage "
                + "the farther it fell, rewarding shots fired from high ground.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Base damage", (int) baseDamage + " HP"),
                new AbilityStat("Fall bonus", "+" + (int) damagePerFiveBlocks + " per 5 blocks"),
                new AbilityStat("Max damage", (int) maxDamage + " HP"),
                new AbilityStat("Radius", aoeRadius + " blocks"),
                new AbilityStat("Energy", (int) energyCost + "")
        );
    }
}
