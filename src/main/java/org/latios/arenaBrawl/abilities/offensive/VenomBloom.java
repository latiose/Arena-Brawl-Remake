package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VenomBloom implements Ability {

    private final double baseDamage;
    private final double damagePerStack;
    private final int maxStacks;
    private final double energyCost;
    private final double maxDistance;
    private final double projectileSpeed;
    private final double gravity;
    private final double radius;
    private final long durationTicks;
    private final long damageIntervalTicks;
    private final long antiHealTicks;
    private final AbilityConfig config;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;

    public VenomBloom(EnergyManager energyManager, TeamManager teamManager,
                      CombatService combatService, DebuffManager debuffManager,
                      AbilityConfig config) {
        this.baseDamage = config.getDouble("base-damage", 8.0);
        this.damagePerStack = config.getDouble("damage-per-stack", 4.0);
        this.maxStacks = (int) config.getLong("max-stacks", 6L);
        this.energyCost = config.getDouble("energy-cost", 35.0);
        this.maxDistance = config.getDouble("max-target-distance", 18.0);
        this.projectileSpeed = config.getDouble("projectile-speed", 1.2);
        this.gravity = config.getDouble("projectile-gravity", 0.03);
        this.radius = config.getDouble("radius", 3.5);
        this.durationTicks = config.getLong("duration-ticks", 100L);
        this.damageIntervalTicks = config.getLong("damage-interval-ticks", 10L);
        this.antiHealTicks = config.getLong("antiheal-duration-ticks", 60L);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Venom Bloom"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Throws a seed that bursts into a poison cloud. Enemies who stay inside "
                + "take progressive damage and have their healing negated.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) baseDamage + " (+" + (int) damagePerStack + "/pulse, max " + maxStacks + ")"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Duration", (durationTicks * 0.05) + "s"),
                new AbilityStat("Radius", radius + " blocks"),
                new AbilityStat("AntiHeal", (antiHealTicks * 0.05) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location start = player.getEyeLocation();
        Vector velocity = start.getDirection().normalize().multiply(projectileSpeed);

        MatchSoundUtils.play(config, player, Sound.ENTITY_SPLASH_POTION_THROW, 1.0f, 0.8f);

        new BukkitRunnable() {
            final Location current = start.clone();
            double traveled = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                velocity.setY(velocity.getY() - gravity);
                double step = velocity.length();

                RayTraceResult hit = current.getWorld().rayTrace(
                        current,
                        velocity.clone().normalize(),
                        step,
                        FluidCollisionMode.NEVER,
                        true,
                        0.3,
                        entity -> entity instanceof Player p
                                && !p.equals(player)
                                && !p.isDead()
                                && teamManager.isEnemy(player, p)
                );

                if (hit != null) {
                    cancel();
                    Location impact = hit.getHitPosition().toLocation(current.getWorld());
                    createCloud(player, impact);
                    return;
                }

                current.add(velocity);
                traveled += step;

                current.getWorld().spawnParticle(Particle.DUST, current, 3, 0.05, 0.05, 0.05,
                        new Particle.DustOptions(Color.fromRGB(90, 200, 60), 1.2f));

                if (traveled >= maxDistance) {
                    cancel();
                    createCloud(player, current.clone());
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    private void createCloud(Player caster, Location center) {
        center.getWorld().playSound(center, Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 0.7f);
        MatchSoundUtils.play(config, caster, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 0.6f);
        center.getWorld().spawnParticle(Particle.DUST, center, 40, 0.5, 0.4, 0.5,
                new Particle.DustOptions(Color.fromRGB(70, 180, 50), 1.6f));

        Map<UUID, Integer> stacks = new HashMap<>();

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed++;

                if (ticksElapsed > durationTicks || !caster.isOnline()) {
                    cancel();
                    return;
                }

                if (ticksElapsed % 2 == 0) {
                    drawCloudParticles(center);
                }
                if (ticksElapsed % 20 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.5f, 0.8f);
                }

                // Detect enemies inside the cloud
                Set<UUID> inside = new HashSet<>();
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (!(entity instanceof Player victim)) continue;
                    if (victim.equals(caster) || victim.isDead()) continue;
                    if (!teamManager.isEnemy(caster, victim)) continue;
                    if (victim.getLocation().distanceSquared(center) > radius * radius) continue;

                    inside.add(victim.getUniqueId());

                    if (!debuffManager.hasDebuff(victim, DebuffType.ANTIHEAL)) {
                        debuffManager.tryApply(
                                caster,
                                victim,
                                DebuffType.ANTIHEAL,
                                antiHealTicks * 50L
                        );
                    }
                }

                // Anyone who leaves the cloud loses their stacks
                stacks.keySet().retainAll(inside);

                // Progressive damage pulse
                if (ticksElapsed % damageIntervalTicks == 0) {
                    for (UUID id : inside) {
                        Player victim = ArenaBrawlPlugin.getInstance().getServer().getPlayer(id);
                        if (victim == null || victim.isDead()) continue;

                        int stack = Math.min(stacks.merge(id, 1, Integer::sum), maxStacks);
                        stacks.put(id, stack);

                        double dmg = baseDamage + damagePerStack * (stack - 1);
                        combatService.applyAbilityDamage(caster, victim, dmg, getName(), center);
                        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0),
                                8, 0.3, 0.5, 0.3,
                                new Particle.DustOptions(Color.fromRGB(60, 150, 40), 1.0f));
                    }
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
    }

    private void drawCloudParticles(Location center) {
        // Outer ring
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            Location p = center.clone().add(radius * Math.cos(angle), 0.1, radius * Math.sin(angle));
            p.getWorld().spawnParticle(Particle.DUST, p, 1,
                    new Particle.DustOptions(Color.fromRGB(80, 190, 50), 1.0f));
        }
        // Inner mist
        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 0.8, 0), 12,
                radius * 0.5, 0.6, radius * 0.5,
                new Particle.DustOptions(Color.fromRGB(100, 210, 70), 1.8f));
        center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, center.clone().add(0, 1.0, 0), 6,
                radius * 0.5, 0.6, radius * 0.5, 0.0);
    }
}