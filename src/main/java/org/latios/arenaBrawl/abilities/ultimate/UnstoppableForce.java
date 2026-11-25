package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class UnstoppableForce implements Ability {

    private final Plugin plugin;
    private final double maxRange;
    private final double impactRadius;
    private final double damage;
    private final long stunDurationMs;
    private final int dashTicks;
    private final long chargeTimeMillis;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;

    public UnstoppableForce(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                            TeamManager teamManager, CombatService combatService,
                            DebuffManager debuffManager, AbilityConfig config) {
        this.plugin = plugin;
        this.maxRange = config.getDouble("max-range", 22.0);
        this.impactRadius = config.getDouble("impact-radius", 5.0);
        this.damage = config.getDouble("damage", 350.0);
        this.stunDurationMs = config.getLong("stun-duration-ms", 1500L);
        this.dashTicks = Math.max(1, config.getInt("dash-ticks", 5));
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "unstoppableforce");
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() {
        return "Unstoppable Force";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "unstoppableforce", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        final Location start = player.getLocation().clone();
        final Location destination = safeDestination(resolveDestination(player));
        destination.setDirection(start.getDirection());

        player.getWorld().playSound(start, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
        player.getWorld().playSound(start, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.7f);

        final int totalTicks = dashTicks;

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR) {
                    cancel();
                    return;
                }

                tick++;

                if (tick >= totalTicks) {
                    player.teleport(destination);
                    player.setFallDistance(0);
                    impact(player, destination);
                    cancel();
                    return;
                }

                double progress = (double) tick / totalTicks;
                Location point = start.clone().add(
                        destination.toVector().subtract(start.toVector()).multiply(progress));
                point.setDirection(destination.getDirection());
                player.teleport(point);
                player.setFallDistance(0);
                drawTrail(point);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Location resolveDestination(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        RayTraceResult result = player.getWorld().rayTraceBlocks(eye, direction, maxRange);
        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition().toLocation(player.getWorld())
                    .subtract(direction.clone().multiply(0.6));
        }

        return eye.clone().add(direction.multiply(maxRange));
    }

    private Location safeDestination(Location base) {
        Location destination = base.clone();

        for (int i = 0; i < 4 && isBlocked(destination); i++) {
            destination.add(0, 1, 0);
        }

        return destination;
    }

    private boolean isBlocked(Location location) {
        return location.getBlock().getType().isSolid()
                || location.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private void drawTrail(Location point) {
        Location center = point.clone().add(0, 1.0, 0);
        center.getWorld().spawnParticle(Particle.DUST, center, 8, 0.35, 0.4, 0.35, 0.0,
                new Particle.DustOptions(Color.fromRGB(110, 110, 120), 1.4f));
        center.getWorld().spawnParticle(Particle.CRIT, center, 3, 0.25, 0.3, 0.25, 0.05);
    }

    private void impact(Player caster, Location center) {
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 6, 0.6, 0.2, 0.6, 0);
        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 0.5, 0), 70,
                impactRadius * 0.45, 0.4, impactRadius * 0.45, 0.0,
                new Particle.DustOptions(Color.fromRGB(110, 110, 120), 1.8f));
        center.getWorld().spawnParticle(Particle.CRIT, center.clone().add(0, 0.5, 0), 40,
                impactRadius * 0.4, 0.3, impactRadius * 0.4, 0.15);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.4f, 0.7f);

        for (Entity nearby : center.getWorld().getNearbyEntities(
                center, impactRadius, impactRadius, impactRadius)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.equals(caster) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (target.getLocation().distanceSquared(center) > impactRadius * impactRadius) continue;

            debuffManager.tryApply(caster, target, DebuffType.STUN, stunDurationMs);
            combatService.applyAbilityDamage(caster, target, damage, getName(), center);

            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0),
                    20, 0.35, 0.6, 0.35, 0.15);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        }
    }

    @Override
    public String getDescription() {
        return "Charges unstoppably toward the targeted location, dealing heavy damage and stunning "
                + "every enemy in the impact area for " + (stunDurationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Stun", (stunDurationMs / 1000L) + "s"),
                new AbilityStat("Impact Radius", (int) impactRadius + " blocks"),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}
