package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
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
import org.latios.arenaBrawl.abilities.structures.PlacedStructure;
import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;


public class AnvilFall implements Ability {

    private final Plugin plugin;
    private final double maxRange;
    private final double spawnHeightOffset;
    private final double impactRadius;
    private final double knockbackStrength;
    private final double fallVelocity;
    private final double directHitRadius;
    private final double damage;
    private final double directHitBonus;
    private final double energyCost;
    private final double verticalReach;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final StructureDemolitionService demolitionService;
    private final AbilityConfig config;

    public AnvilFall(Plugin plugin, org.latios.arenaBrawl.general.EnergyManager energyManager,
                     TeamManager teamManager, CombatService combatService,
                     StructureDemolitionService demolitionService, AbilityConfig config) {
        this.plugin = plugin;
        this.maxRange = config.getDouble("max-range", 15.0);
        this.spawnHeightOffset = config.getDouble("spawn-height-offset", 6.0);
        this.impactRadius = config.getDouble("impact-radius", 4.0);
        this.knockbackStrength = config.getDouble("knockback-strength", 1.0);
        this.fallVelocity = config.getDouble("fall-velocity", 1.0);
        this.directHitRadius = config.getDouble("direct-hit-radius", 1.5);
        this.damage = config.getDouble("damage", 220.0);
        this.directHitBonus = config.getDouble("direct-hit-bonus", 30.0);
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.verticalReach = config.getDouble("vertical-reach", 15.0);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.demolitionService = demolitionService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Anvil Fall";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Drops an explosive anvil where you target, dealing " + (int) damage + " damage to all enemy "
                + "players and knocking them back. Destroys hit structures. A direct hit causes an additional "
                + (int) directHitBonus + " damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Direct Hit Bonus", "+" + (int) directHitBonus),
                new AbilityStat("Explosion Radius", (int) impactRadius + "m"),
                new AbilityStat("Knockback Distance", (int) knockbackStrength + "m"),
                new AbilityStat("Max Range", (int) maxRange + "m"),
                new AbilityStat("Breaks structures", "Yes")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location target = resolveTargetLocation(player);
        Location spawnLocation = target.clone().add(0, spawnHeightOffset, 0);

        FallingBlock anvil = spawnLocation.getWorld().spawnFallingBlock(spawnLocation, Material.ANVIL.createBlockData());
        anvil.setDropItem(false);
        anvil.setHurtEntities(false);
        anvil.setCancelDrop(true);

        EntityCleanupUtils.markAsArenaEntity(anvil);

        new BukkitRunnable() {
            int ticks = 0;
            Location lastLocation = spawnLocation.clone();

            @Override
            public void run() {
                if (anvil.isValid() && !anvil.isDead()) {
                    lastLocation = anvil.getLocation();

                    // Twice the vanilla fall speed: pin a constant downward velocity
                    // every tick.
                    anvil.setVelocity(new Vector(0, -fallVelocity, 0));

                    boolean hitGround = anvil.isOnGround()
                            || lastLocation.getBlock().getType().isSolid()
                            || lastLocation.clone().subtract(0, 0.3, 0).getBlock().getType().isSolid();

                    ticks++;

                    if (hitGround || ticks >= 40) {
                        triggerImpact(player, lastLocation);
                        cleanAnvilBlocks(lastLocation);
                        anvil.remove();
                        cancel();
                        return;
                    }

                    return;
                }

                triggerImpact(player, lastLocation);
                cleanAnvilBlocks(lastLocation);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Location resolveTargetLocation(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), maxRange
        );

        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition().toLocation(player.getWorld());
        }

        Vector direction = player.getEyeLocation().getDirection().normalize().multiply(maxRange);
        return player.getEyeLocation().add(direction);
    }

    private void triggerImpact(Player caster, Location impactLocation) {
        impactLocation.getWorld().spawnParticle(Particle.EXPLOSION, impactLocation, 3);
        impactLocation.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                impactLocation.clone().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 0.0);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_ANVIL_LAND, 10.0f, 0.8f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 1.0f);

        cleanAnvilBlocks(impactLocation);

        List<PlacedStructure> nearbyStructures = demolitionService.findStructuresInRadius(impactLocation, impactRadius);
        for (PlacedStructure structure : nearbyStructures) {
            if (demolitionService.isEnemyStructure(caster, structure)) {
                demolitionService.demolish(structure, impactLocation);
            }
        }

        double minY = impactLocation.getY() - verticalReach;
        double maxY = impactLocation.getY() + impactRadius;
        double searchHalfHeight = Math.max(verticalReach, impactRadius);
        double radiusSquared = impactRadius * impactRadius;

        for (Entity nearby : impactLocation.getWorld().getNearbyEntities(
                impactLocation, impactRadius, searchHalfHeight, impactRadius)) {

            if (!(nearby instanceof Player target) || !teamManager.isEnemy(caster, target)) {
                continue;
            }

            Location targetLoc = target.getLocation();

            if (targetLoc.getY() < minY || targetLoc.getY() > maxY) {
                continue;
            }

            double dx = targetLoc.getX() - impactLocation.getX();
            double dz = targetLoc.getZ() - impactLocation.getZ();
            if (dx * dx + dz * dz > radiusSquared) {
                continue;
            }

            Vector direction = new Vector(dx, 0, dz);
            if (direction.lengthSquared() == 0) {
                direction = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }

            Vector knockback = direction.normalize().multiply(knockbackStrength);
            knockback.setY(0.7);
            target.setVelocity(knockback);

            boolean directHit = targetLoc.distance(impactLocation) <= directHitRadius;
            combatService.applyAbilityDamage(caster, target,
                    directHit ? damage + directHitBonus : damage, "Anvil Fall");

            target.getWorld().spawnParticle(Particle.EXPLOSION,
                    targetLoc.clone().add(0, 1, 0), 1);
        }
    }

    private void cleanAnvilBlocks(Location impactLocation) {
        cleanAnvilBlock(impactLocation.getBlock());
        cleanAnvilBlock(impactLocation.clone().subtract(0, 1, 0).getBlock());
    }

    private void cleanAnvilBlock(org.bukkit.block.Block block) {
        if (block.getType().name().endsWith("ANVIL")) {
            block.setType(Material.AIR);
        }
    }
}
