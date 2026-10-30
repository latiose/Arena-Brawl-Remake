package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aerial dive. Only usable while airborne: the caster is thrown straight down
 * and the landing creates an area shockwave whose damage grows with the height
 * of the dive.
 */
public class Plunge implements Ability {

    private final double energyCost;
    private final double baseDamage;
    private final double damagePerBlock;
    private final double maxDamage;
    private final double radius;
    private final double knockUp;
    private final double minFall;
    private final double diveSpeed;
    private final int maxTicks;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Set<UUID> plunging = ConcurrentHashMap.newKeySet();

    public Plunge(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                  CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.baseDamage = config.getDouble("base-damage", 140.0);
        this.damagePerBlock = config.getDouble("damage-per-block", 20.0);
        this.maxDamage = config.getDouble("max-damage", 260.0);
        this.radius = config.getDouble("radius", 4.0);
        this.knockUp = config.getDouble("knock-up", 0.5);
        this.minFall = config.getDouble("min-fall", 1.0);
        this.diveSpeed = config.getDouble("dive-speed", 1.5);
        this.maxTicks = config.getInt("max-ticks", 60);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Plunge";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        UUID id = player.getUniqueId();
        if (plunging.contains(id)) return false;
        if (player.isOnGround()) return false;
        if (player.getFallDistance() < minFall && player.getVelocity().getY() > -0.05) {
            return false;
        }

        plunging.add(id);
        Location start = player.getLocation().clone();
        player.setVelocity(new Vector(0, -diveSpeed, 0));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 0.7f, 1.4f);

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR) {
                    plunging.remove(id);
                    cancel();
                    return;
                }

                ticks++;

                if (player.isOnGround() || ticks >= maxTicks) {
                    impact(player, start);
                    plunging.remove(id);
                    cancel();
                    return;
                }

                Location location = player.getLocation();
                location.getWorld().spawnParticle(
                        Particle.CRIT, location, 4, 0.2, 0.1, 0.2, 0.03
                );
                location.getWorld().spawnParticle(
                        Particle.CLOUD, location.clone().add(0, 0.3, 0),
                        2, 0.2, 0.05, 0.2, 0.01
                );
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    private void impact(Player player, Location start) {
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) return;

        double fall = Math.max(0.0, start.getY() - center.getY());
        double damage = Math.min(maxDamage, baseDamage + fall * damagePerBlock);

        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.9f, 0.7f);
        world.spawnParticle(Particle.EXPLOSION, center.clone().add(0, 0.5, 0), 2, 0.2, 0.1, 0.2, 0.0);
        world.spawnParticle(Particle.CRIT, center.clone().add(0, 0.5, 0), 35, radius * 0.5, 0.4, radius * 0.5, 0.15);
        world.spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 0.3, 0), 6, radius * 0.5, 0.1, radius * 0.5, 0.0);

        double radiusSq = radius * radius;
        for (Entity nearby : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.equals(player) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, target)) continue;
            if (target.getLocation().add(0, 0.25, 0).distanceSquared(center) > radiusSq) continue;

            combatService.applyAbilityDamage(player, target, damage, getName(), center);

            Vector velocity = target.getVelocity();
            target.setVelocity(new Vector(velocity.getX(), knockUp, velocity.getZ()));
        }
    }

    @Override
    public String getDescription() {
        return "While airborne, dive straight down and crash into the ground. "
                + "The shockwave damages nearby enemies and knocks them up, scaling with the fall height.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) baseDamage + " HP"),
                new AbilityStat("Fall bonus", "+" + (int) damagePerBlock + " per block"),
                new AbilityStat("Max damage", (int) maxDamage + " HP"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Energy", (int) energyCost + "")
        );
    }
}
