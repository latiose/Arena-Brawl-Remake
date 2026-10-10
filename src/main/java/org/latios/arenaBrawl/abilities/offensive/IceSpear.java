package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.List;

public class IceSpear implements Ability {

    private final double damage;
    private final double energyCost;
    private final double maxDistance;
    private final double projectileSpeed;
    private final double radius;
    private final long slowTicks;
    private final AbilityConfig config;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;

    public IceSpear(EnergyManager energyManager, TeamManager teamManager,
                    CombatService combatService, DebuffManager debuffManager,
                    AbilityConfig config) {
        this.damage = config.getDouble("damage", 70.0);
        this.energyCost = config.getDouble("energy-cost", 30.0);
        this.maxDistance = config.getDouble("max-target-distance", 40.0);
        this.projectileSpeed = config.getDouble("projectile-speed", 3.5);
        this.radius = config.getDouble("radius", 2.0);
        this.slowTicks = config.getLong("slow-duration-ticks", 60L);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Ice Spear"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Hurls a very fast ice spear that shatters on impact, damaging and slowing enemies in the area.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Radius", radius + " blocks"),
                new AbilityStat("Slow", (slowTicks * 0.05) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location current = player.getEyeLocation().clone();
        Vector direction = current.getDirection().normalize();

        MatchSoundUtils.play(config, player, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.6f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_GLASS_BREAK, 0.5f, 2.0f);

        new BukkitRunnable() {
            double traveled = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                double step = Math.min(projectileSpeed, maxDistance - traveled);

                RayTraceResult hit = current.getWorld().rayTrace(
                        current,
                        direction,
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
                    drawTrail(current, direction, current.distance(impact));
                    shatter(player, impact);
                    return;
                }

                drawTrail(current, direction, step);
                current.add(direction.clone().multiply(step));
                traveled += step;

                if (traveled >= maxDistance) {
                    cancel();
                    shatter(player, current.clone());
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    /** Draws the spear body and trail along the path travelled this tick. */
    private void drawTrail(Location from, Vector direction, double length) {
        for (double d = 0; d <= length; d += 0.4) {
            Location p = from.clone().add(direction.clone().multiply(d));
            p.getWorld().spawnParticle(Particle.DUST, p, 1, 0.0, 0.0, 0.0,
                    new Particle.DustOptions(Color.fromRGB(170, 230, 255), 1.3f));
            p.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 1, 0.05, 0.05, 0.05, 0.0);
        }
        // Bright spear tip
        Location tip = from.clone().add(direction.clone().multiply(length));
        tip.getWorld().spawnParticle(Particle.END_ROD, tip, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private void shatter(Player caster, Location impact) {
        impact.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 60, radius * 0.4, 0.5, radius * 0.4, 0.1);
        impact.getWorld().spawnParticle(Particle.BLOCK, impact, 40, radius * 0.4, 0.4, radius * 0.4, 0.1,
                Material.ICE.createBlockData());
        impact.getWorld().spawnParticle(Particle.END_ROD, impact, 15, 0.3, 0.3, 0.3, 0.15);
        drawImpactRing(impact);

        impact.getWorld().playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
        MatchSoundUtils.play(config, caster, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);

        for (Entity entity : impact.getWorld().getNearbyEntities(impact, radius, radius, radius)) {
            if (!(entity instanceof Player victim)) continue;
            if (victim.equals(caster) || victim.isDead()) continue;
            if (!teamManager.isEnemy(caster, victim)) continue;

            // Measure from the victim's body center so it works for direct hits too
            Location body = victim.getLocation().add(0, 1.0, 0);
            if (body.distanceSquared(impact) > radius * radius) continue;

            combatService.applyAbilityDamage(caster, victim, damage, getName(), impact);
            debuffManager.tryApply(caster, victim, DebuffType.SLOW, slowTicks * 50L);

            victim.getWorld().spawnParticle(Particle.SNOWFLAKE, body, 20, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private void drawImpactRing(Location center) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            Location p = center.clone().add(radius * Math.cos(angle), 0.1, radius * Math.sin(angle));
            p.getWorld().spawnParticle(Particle.DUST, p, 1, 0.0, 0.0, 0.0,
                    new Particle.DustOptions(Color.fromRGB(150, 220, 255), 1.2f));
        }
    }
}