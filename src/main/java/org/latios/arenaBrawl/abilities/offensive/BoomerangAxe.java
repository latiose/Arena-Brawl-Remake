package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BoomerangAxe implements Ability {

    private final double damage;
    private final double energyCost;
    private final double maxDistance;
    private final double projectileSpeed;
    private final double hitRadius;
    private final double groundHitRadius;
    private final double catchRadius;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityConfig config;

    public BoomerangAxe(EnergyManager energyManager, TeamManager teamManager,
                        CombatService combatService, AbilityConfig config) {
        this.damage = config.getDouble("damage", 80.0);
        this.energyCost = config.getDouble("energy-cost", 50.0);
        this.maxDistance = config.getDouble("max-distance", 22.0);
        this.projectileSpeed = config.getDouble("projectile-speed", 1.4);
        this.hitRadius = config.getDouble("hit-radius", 1.6);
        this.groundHitRadius = config.getDouble("ground-hit-radius", 2.8);
        this.catchRadius = config.getDouble("catch-radius", 1.6);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Boomerang Axe";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Throws a spinning axe that damages enemies on the way out and again on the way back.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage) + " per pass"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("Range", (int) maxDistance + " blocks")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location start = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector outbound = start.getDirection().normalize();

        ItemDisplay axe = start.getWorld().spawn(start, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.IRON_AXE));
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setPersistent(false);
            display.setTeleportDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.1f, 1.1f, 1.1f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });
        EntityCleanupUtils.markAsArenaEntity(axe);

        MatchSoundUtils.play(config, player, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.7f);
        MatchSoundUtils.play(config, player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.8f);

        new BukkitRunnable() {
            private final Location current = start.clone();
            private Vector direction = outbound.clone();
            private boolean returning = false;
            private double traveled = 0;
            private int ticks = 0;
            private final Set<UUID> hitThisPass = new HashSet<>();

            @Override
            public void run() {
                ticks++;
                if (!axe.isValid() || ticks > 80 || !player.isOnline() || player.isDead()) {
                    cleanup();
                    return;
                }

                if (returning) {
                    Vector toThrower = player.getEyeLocation().toVector().subtract(current.toVector());
                    if (toThrower.lengthSquared() <= catchRadius * catchRadius) {
                        MatchSoundUtils.play(config, player, Sound.ITEM_ARMOR_EQUIP_IRON, 0.8f, 1.4f);
                        cleanup();
                        return;
                    }
                    if (toThrower.lengthSquared() > 0.0001) {
                        direction = toThrower.normalize();
                    }
                }

                Location from = current.clone();
                Location next = current.clone().add(direction.clone().multiply(projectileSpeed));
                if (!returning && next.getBlock().getType().isSolid()) {
                    handleGroundHit(next);
                    return;
                }

                current.add(direction.clone().multiply(projectileSpeed));
                if (!returning) {
                    traveled += projectileSpeed;
                }

                axe.teleport(current);
                axe.setRotation(ticks * 48f, current.getPitch());
                current.getWorld().spawnParticle(Particle.SWEEP_ATTACK, current, 1, 0, 0, 0, 0);
                current.getWorld().spawnParticle(Particle.DUST, current, 2, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 180, 190), 1.0f));

                damageNearby(from, current);

                if (!returning && traveled >= maxDistance) {
                    beginReturn();
                }
            }

            private void beginReturn() {
                returning = true;
                hitThisPass.clear();
                MatchSoundUtils.play(config, player, Sound.ITEM_TRIDENT_RETURN, 0.7f, 1.4f);
            }

            private void handleGroundHit(Location impact) {
                Location effectLoc = impact.clone().add(0, 0.15, 0);
                effectLoc.getWorld().spawnParticle(Particle.EXPLOSION, effectLoc, 1, 0, 0, 0, 0);
                effectLoc.getWorld().spawnParticle(Particle.CRIT, effectLoc, 20, 0.4, 0.2, 0.4, 0.08);
                effectLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 6, 0.4, 0.1, 0.4, 0.0);
                MatchSoundUtils.play(config, player, Sound.ITEM_AXE_STRIP, 1.0f, 0.5f);
                MatchSoundUtils.play(config, player, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 0.9f);

                damageAround(effectLoc, groundHitRadius);
                beginReturn();
            }

            private void damageNearby(Location from, Location to) {
                for (Player target : to.getWorld().getPlayers()) {
                    if (target.equals(player) || target.isDead()) continue;
                    if (!teamManager.isEnemy(player, target)) continue;
                    if (hitThisPass.contains(target.getUniqueId())) continue;
                    if (!segmentIntersectsBox(target.getBoundingBox(), from.toVector(), to.toVector(), hitRadius)) {
                        continue;
                    }

                    hitThisPass.add(target.getUniqueId());
                    applyHit(target, to);
                }
            }

            private void damageAround(Location center, double radius) {
                for (Player target : center.getWorld().getPlayers()) {
                    if (target.equals(player) || target.isDead()) continue;
                    if (!teamManager.isEnemy(player, target)) continue;
                    if (hitThisPass.contains(target.getUniqueId())) continue;
                    if (target.getLocation().add(0, 1.0, 0).distanceSquared(center) > radius * radius) {
                        continue;
                    }

                    hitThisPass.add(target.getUniqueId());
                    applyHit(target, center);
                }
            }

            private void applyHit(Player target, Location impact) {
                combatService.applyAbilityDamage(player, target, damage, getName(), impact);
                target.getWorld().playSound(target.getLocation(), Sound.ITEM_AXE_STRIP, 1.0f, 0.8f);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0),
                        12, 0.2, 0.3, 0.2, 0.05);
            }

            private void cleanup() {
                if (axe.isValid()) {
                    axe.remove();
                }
                cancel();
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    /**
     * Checks whether the sweep from {@code start} to {@code end} intersects the player's
     * bounding box, expanded by {@code padding}. This replaces the old single-point check so
     * fast-moving axes no longer tunnel through players between ticks.
     */
    private static boolean segmentIntersectsBox(BoundingBox box, Vector start, Vector end, double padding) {
        BoundingBox expanded = box.clone().expand(padding);
        if (expanded.contains(start)) {
            return true;
        }

        Vector delta = end.clone().subtract(start);
        double length = delta.length();
        if (length < 1.0e-6) {
            return false;
        }

        Vector direction = delta.multiply(1.0 / length);
        RayTraceResult hit = expanded.rayTrace(start, direction, length);
        return hit != null;
    }
}
