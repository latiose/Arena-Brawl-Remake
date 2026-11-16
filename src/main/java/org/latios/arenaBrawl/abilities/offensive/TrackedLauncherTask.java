package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.structures.ProjectileInterceptionService;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

public class TrackedLauncherTask extends BukkitRunnable {

    private final ArmorStand armorStand;
    private final Player shooter;
    private final double mainDamage;
    private final double sliceDamage;
    private final double aoeRadius;
    private final String abilityName;
    private final Material sliceMaterial;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private Vector velocity;
    private Location previousCenter;
    private static final double RESTITUTION = 0.55;
    private static final double SURFACE_FRICTION = 0.8;
    private static final double AIR_DRAG = 0.995;
    private static final double MIN_BOUNCE_SPEED = 0.08;

    public TrackedLauncherTask(ArmorStand armorStand, Vector velocity, Player shooter, double mainDamage, double sliceDamage,
                               double aoeRadius, String abilityName, Material headMaterial, Material sliceMaterial,
                               TeamManager teamManager, CombatService combatService) {
        this.armorStand = armorStand;
        this.velocity = velocity;
        this.shooter = shooter;
        this.mainDamage = mainDamage;
        this.sliceDamage = sliceDamage;
        this.aoeRadius = aoeRadius;
        this.abilityName = abilityName;
        this.sliceMaterial = sliceMaterial;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.previousCenter = armorStand.getLocation().add(0, 1.4, 0);
    }

    @Override
    public void run() {

        if (!armorStand.isValid()) {
            cancel();
            armorStand.remove();
            return;
        }

        velocity.multiply(AIR_DRAG);
        velocity.setY(velocity.getY() - 0.025);
        Location previous = previousCenter.clone();
        Location nextLoc = armorStand.getLocation().add(velocity);
        Location projectileCenter = nextLoc.clone().add(0, 1.4, 0);

        Vector travel = projectileCenter.toVector().subtract(previous.toVector());
        if (travel.lengthSquared() > 1.0E-6) {
            RayTraceResult blockHit = previous.getWorld().rayTraceBlocks(
                    previous, travel.clone().normalize(), travel.length(),
                    FluidCollisionMode.NEVER, true);

            if (blockHit != null) {
                BlockFace face = blockHit.getHitBlockFace();
                if (face == null || face == BlockFace.UP) {
                    explode(null, blockHit.getHitPosition().toLocation(armorStand.getWorld()));
                } else {
                    bounce(blockHit);
                }
                return;
            }
        }

        armorStand.teleport(nextLoc);

        if (ProjectileInterceptionService.tryIntercept(shooter, previous, projectileCenter)) {
            cancel();
            armorStand.remove();
            return;
        }

        Player directHit = null;
        Location directImpact = projectileCenter;
        for (Player candidate : armorStand.getWorld().getPlayers()) {
            if (candidate.equals(shooter) || !teamManager.isEnemy(shooter, candidate)) continue;

            BoundingBox targetBox = candidate.getBoundingBox().expand(0.5);
            if (targetBox.contains(projectileCenter.toVector())) {
                directHit = candidate;
                break;
            }

            Vector movement = projectileCenter.toVector().subtract(previous.toVector());
            if (movement.lengthSquared() > 1.0E-6) {
                RayTraceResult hit = targetBox.rayTrace(
                        previous.toVector(), movement.clone().normalize(), movement.length());
                if (hit != null) {
                    directHit = candidate;
                    directImpact = hit.getHitPosition().toLocation(armorStand.getWorld());
                    break;
                }
            }
        }

        previousCenter = projectileCenter.clone();
        if (directHit != null) {
            explode(directHit, directImpact);
        }
    }

    private void bounce(RayTraceResult hit) {
        Vector normal = hit.getHitBlockFace().getDirection();
        double impactSpeed = -velocity.dot(normal);

        Vector normalComponent = normal.clone().multiply(velocity.dot(normal));
        Vector tangentComponent = velocity.clone().subtract(normalComponent).multiply(SURFACE_FRICTION);

        if (impactSpeed < MIN_BOUNCE_SPEED) {
            velocity = tangentComponent;
        } else {
            velocity = tangentComponent.subtract(normalComponent.multiply(RESTITUTION));
        }

        Location center = hit.getHitPosition().toLocation(armorStand.getWorld())
                .add(normal.clone().multiply(0.1));
        armorStand.teleport(center.clone().subtract(0, 1.4, 0));
        previousCenter = center;

        float volume = (float) Math.min(1.0, 0.3 + impactSpeed * 1.5);
        float pitch = (float) (1.0 + Math.min(0.6, impactSpeed) * 0.5 + (Math.random() - 0.5) * 0.2);
        center.getWorld().playSound(center, Sound.BLOCK_STONE_HIT, volume, pitch);
    }

    private void explode(Player directHitVictim, Location impactLoc) {
        cancel();

        if (directHitVictim != null) {
            combatService.applyAbilityDamage(shooter, directHitVictim, mainDamage, abilityName);
        }

        for (Entity nearby : armorStand.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && !target.equals(directHitVictim)
                    && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, mainDamage, abilityName);
            }
        }


        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1);
        impactLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, impactLoc, 40, 0.5, 0.5, 0.5, 0.2);
        impactLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, impactLoc, 30, 0.4, 0.4, 0.4, 0.1);

        for (Player p : impactLoc.getWorld().getPlayers()) {
            impactLoc.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 10f
                    , 1);
            impactLoc.getWorld().playSound(p.getLocation(), Sound.BLOCK_WOOD_BREAK, 10f, 1.5f);
        }


        spawnSlices(impactLoc);

        armorStand.remove();
    }

    private void spawnSlices(Location impactLoc) {
        Location spawnLoc = impactLoc.clone();

        for (int i = 0; i < 3; i++) {
            Item slice = spawnLoc.getWorld().dropItem(spawnLoc, new ItemStack(sliceMaterial));
            slice.setPickupDelay(Integer.MAX_VALUE);
            slice.setVelocity(new Vector(0, 0.5, 0));

            spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_SLIME_BLOCK_BREAK, 10f, 0.8f);

            new TrackedLauncherSliceTask(slice, shooter, sliceDamage, abilityName, teamManager, combatService)
                    .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        }
    }
}