package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Sticky projectile. It attaches to the first enemy hit (or to the ground) and
 * detonates after a short fuse. If the victim has an ally nearby, the blast is
 * split between both targets instead of hitting one target for full damage.
 */
public class ContagionBomb implements Ability {

    private final double energyCost;
    private final double impactDamage;
    private final double splitDamage;
    private final double groundDamage;
    private final double allyRadius;
    private final double groundRadius;
    private final double projectileSpeed;
    private final double hitboxRadius;
    private final long fuseMs;
    private final int maxFlightTicks;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityConfig config;

    public ContagionBomb(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                         CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 60.0);
        this.impactDamage = config.getDouble("impact-damage", 180.0);
        this.splitDamage = config.getDouble("split-damage", 120.0);
        this.groundDamage = config.getDouble("ground-damage", 120.0);
        this.allyRadius = config.getDouble("ally-radius", 4.0);
        this.groundRadius = config.getDouble("ground-radius", 3.0);
        this.projectileSpeed = config.getDouble("projectile-speed", 1.6);
        this.hitboxRadius = Math.max(0.35, config.getDouble("hitbox-radius", 1.9));
        this.fuseMs = config.getLong("fuse-ms", 2500L);
        this.maxFlightTicks = config.getInt("max-flight-ticks", 80);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Contagion Bomb";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location spawn = eye.clone().add(direction.clone().multiply(1.2));

        ItemDisplay bomb = player.getWorld().spawn(spawn, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.TNT));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            display.setBillboard(Display.Billboard.CENTER);
            display.setViewRange(32.0f);
            display.setTeleportDuration(0);
        });
        EntityCleanupUtils.markAsArenaEntity(bomb);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.7f);

        new BukkitRunnable() {
            int ticks = 0;
            final Vector step = direction.clone().multiply(projectileSpeed);

            @Override
            public void run() {
                if (!bomb.isValid()) {
                    cancel();
                    return;
                }

                if (!player.isOnline()
                        || !config.getMatchManager().isInMatch(player)) {
                    bomb.remove();
                    cancel();
                    return;
                }

                Location current = bomb.getLocation();
                double speed = step.length();
                Vector travel = step.clone().normalize();

                // The server can move players very quickly, so use a generous
                // entity hitbox and extend the sweep by the same radius. This
                // prevents the bomb from tunnelling past a moving target.
                RayTraceResult hit = current.getWorld().rayTrace(
                        current, travel, speed + 0.45 + hitboxRadius,
                        FluidCollisionMode.NEVER, true, hitboxRadius,
                        entity -> entity instanceof Player target
                                && !target.equals(player)
                                && !target.isDead()
                                && target.getGameMode() != GameMode.SPECTATOR
                                && teamManager.isEnemy(player, target)
                );

                if (hit != null) {
                    if (hit.getHitEntity() instanceof Player target
                            && player.hasLineOfSight(target)) {
                        bomb.remove();
                        cancel();
                        stickToTarget(player, target);
                        return;
                    }

                    if (hit.getHitBlock() != null) {
                        Location impact = hit.getHitPosition().toLocation(current.getWorld());
                        bomb.remove();
                        cancel();
                        stickToGround(player, impact);
                        return;
                    }
                }

                current.getWorld().spawnParticle(
                        Particle.SMOKE, current, 3, 0.08, 0.08, 0.08, 0.01
                );
                current.getWorld().spawnParticle(
                        Particle.FLAME, current, 1, 0.04, 0.04, 0.04, 0.0
                );

                bomb.teleport(current.clone().add(step));

                if (++ticks >= maxFlightTicks) {
                    Location last = current.clone();
                    bomb.remove();
                    cancel();
                    stickToGround(player, last);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void stickToTarget(Player caster, Player target) {
        Location attach = target.getLocation().add(0, 1.0, 0);
        attach.getWorld().playSound(attach, Sound.ENTITY_CREEPER_PRIMED, 1.2f, 1.2f);
        attach.getWorld().spawnParticle(Particle.SMOKE, attach, 16, 0.25, 0.35, 0.25, 0.02);

        new BukkitRunnable() {
            final long detonateAt = System.currentTimeMillis() + fuseMs;

            @Override
            public void run() {
                if (!caster.isOnline() || !config.getMatchManager().isInMatch(caster)) {
                    cancel();
                    return;
                }

                if (!target.isOnline() || target.isDead()
                        || target.getGameMode() == GameMode.SPECTATOR) {
                    Location last = target.getLocation().add(0, 1.0, 0);
                    cancel();
                    explodeGround(caster, last);
                    return;
                }

                Location current = target.getLocation().add(0, 1.0, 0);
                current.getWorld().spawnParticle(
                        Particle.SMOKE, current, 4, 0.18, 0.25, 0.18, 0.01
                );
                current.getWorld().spawnParticle(
                        Particle.FLAME, current, 1, 0.08, 0.12, 0.08, 0.0
                );

                if (System.currentTimeMillis() >= detonateAt) {
                    cancel();
                    explodeOnTarget(caster, target);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void stickToGround(Player caster, Location location) {
        Location attach = location.clone();
        attach.getWorld().playSound(attach, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.3f);
        attach.getWorld().spawnParticle(Particle.SMOKE, attach, 12, 0.2, 0.2, 0.2, 0.02);

        new BukkitRunnable() {
            final long detonateAt = System.currentTimeMillis() + fuseMs;

            @Override
            public void run() {
                if (!caster.isOnline() || !config.getMatchManager().isInMatch(caster)) {
                    cancel();
                    return;
                }

                attach.getWorld().spawnParticle(
                        Particle.SMOKE, attach, 3, 0.15, 0.2, 0.15, 0.01
                );

                if (System.currentTimeMillis() >= detonateAt) {
                    cancel();
                    explodeGround(caster, attach);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void explodeOnTarget(Player caster, Player target) {
        Location center = target.getLocation().add(0, 1.0, 0);
        Player nearbyAlly = findNearbyAlly(caster, target);

        if (nearbyAlly != null) {
            applyDamage(caster, target, splitDamage, center);
            applyDamage(caster, nearbyAlly, splitDamage, nearbyAlly.getLocation().add(0, 1.0, 0));
            center.getWorld().spawnParticle(Particle.HEART, center, 8, 0.4, 0.4, 0.4, 0.02);
        } else {
            applyDamage(caster, target, impactDamage, center);
        }

        playExplosion(center);
    }

    private void explodeGround(Player caster, Location center) {
        for (Player target : config.getMatchManager().getMatchPlayers(caster)) {
            if (target.equals(caster) || target.isDead()
                    || target.getGameMode() == GameMode.SPECTATOR
                    || !teamManager.isEnemy(caster, target)) {
                continue;
            }

            if (target.getLocation().distanceSquared(center) <= groundRadius * groundRadius) {
                applyDamage(caster, target, groundDamage, target.getLocation().add(0, 1.0, 0));
            }
        }

        playExplosion(center);
    }

    private Player findNearbyAlly(Player caster, Player target) {
        Player closest = null;
        double closestDistance = allyRadius * allyRadius;

        for (Player candidate : config.getMatchManager().getMatchPlayers(caster)) {
            if (candidate.equals(target) || candidate.equals(caster)
                    || candidate.isDead()
                    || candidate.getGameMode() == GameMode.SPECTATOR
                    || !teamManager.isAlly(target, candidate)) {
                continue;
            }

            double distance = candidate.getLocation().distanceSquared(target.getLocation());
            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }

        return closest;
    }

    private void applyDamage(Player caster, Player victim, double amount, Location impact) {
        if (victim == null || victim.isDead() || victim.getGameMode() == GameMode.SPECTATOR) return;
        combatService.applyAbilityDamage(caster, victim, amount, getName(), impact);
    }

    private void playExplosion(Location center) {
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        center.getWorld().spawnParticle(Particle.SMOKE, center, 30, 0.8, 0.6, 0.8, 0.08);
        center.getWorld().spawnParticle(
                Particle.DUST, center, 24, 0.7, 0.5, 0.7,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(80, 180, 40), 1.4f)
        );
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.9f);
    }

    @Override
    public String getDescription() {
        return "Lobs a sticky bomb that attaches to the first enemy hit. After a short fuse it deals " +
                (int) impactDamage + " damage, or splits into " + (int) splitDamage +
                " damage for the victim and a nearby ally.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Impact Damage", String.valueOf((int) impactDamage)),
                new AbilityStat("Split Damage", (int) splitDamage + " each"),
                new AbilityStat("Ally Radius", String.format("%.1f blocks", allyRadius)),
                new AbilityStat("Ground Damage", String.valueOf((int) groundDamage)),
                new AbilityStat("Hitbox", String.format("%.1f blocks", hitboxRadius)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Fuse", String.format("%.1fs", fuseMs / 1000.0))
        );
    }
}
