package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A solid glass wall that only the owner's team can cross. Enemies collide with the blocks
 * normally; allies are "phased" to the other side when they walk into the wall while facing it.
 */
public class PhaseWallStructure extends PlacedStructure implements DestructibleStructure {

    private static final double PHASE_DISTANCE = 1.15;
    private static final double PHASE_CLEARANCE = 0.75;
    private static final long PHASE_COOLDOWN_MILLIS = 500L;

    private final TeamManager teamManager;
    private final long durationMillis;
    private final int meleeHitsToDestroy;

    private final List<Block> placedBlocks = new ArrayList<>();
    private final Map<UUID, Long> lastPhaseAt = new HashMap<>();

    private final Vector normal;
    private final Vector widthAxis;
    private final Location planeCenter;
    private final double halfWidth;
    private final double height;

    private int remainingHits;

    public PhaseWallStructure(Player owner, Location base, StructureBlueprint blueprint, BlockFace facing,
                              TeamManager teamManager, long durationMillis, int hitsToDestroy) {
        super(owner, base.getBlock().getLocation());
        this.teamManager = teamManager;
        this.durationMillis = durationMillis;
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;

        this.normal = normalFor(facing);
        this.widthAxis = new Vector(normal.getZ(), 0, -normal.getX()).normalize();

        int maxX = 0;
        int maxZ = 0;
        int maxY = 0;
        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            maxX = Math.max(maxX, Math.abs(offset.dx()));
            maxZ = Math.max(maxZ, Math.abs(offset.dz()));
            maxY = Math.max(maxY, offset.dy());
        }

        this.halfWidth = Math.max(maxX, maxZ) + 0.5;
        this.height = maxY + 1;
        this.planeCenter = base.getBlock().getLocation().add(0.5, height / 2.0, 0.5);

        build(base.getBlock().getLocation(), facing, blueprint);
    }

    private void build(Location base, BlockFace facing, StructureBlueprint blueprint) {
        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            int[] rotated = rotateOffset(offset.dx(), offset.dz(), facing);
            Block block = base.clone().add(rotated[0], offset.dy(), rotated[1]).getBlock();

            if (!isReplaceable(block.getType())) continue;
            if (StructureBlockRegistry.isOwnedByOther(block, this)) continue;

            StructureBlockRegistry.claim(block, this);
            block.setType(offset.material());
            placedBlocks.add(block);
        }
    }

    /**
     * Returns the destination on the other side of the wall when an ally walks into it,
     * or {@code null} when the movement should be left untouched.
     */
    public Location resolvePhase(Player player, Location from, Location to) {
        Player owner = getOwner();
        if (owner == null) return null;
        if (!player.equals(owner) && !teamManager.isAlly(owner, player)) return null;

        long now = System.currentTimeMillis();
        if (now - lastPhaseAt.getOrDefault(player.getUniqueId(), 0L) < PHASE_COOLDOWN_MILLIS) {
            return null;
        }

        Vector delta = to.toVector().subtract(planeCenter.toVector());
        double distanceToPlane = delta.dot(normal);
        double lateral = delta.dot(widthAxis);
        double vertical = delta.getY();

        if (Math.abs(distanceToPlane) > PHASE_DISTANCE) return null;
        if (Math.abs(lateral) > halfWidth + 0.35) return null;
        if (vertical < -height / 2.0 - 0.6 || vertical > height / 2.0 + 0.6) return null;

        double side = distanceToPlane >= 0 ? 1 : -1;

        Vector movement = to.toVector().subtract(from.toVector());
        boolean movingTowardWall = movement.dot(normal) * side < -0.001;

        Vector look = player.getEyeLocation().getDirection();
        look.setY(0);
        if (look.lengthSquared() < 1.0E-6) return null;
        look.normalize();
        boolean lookingTowardWall = look.dot(normal) * side < -0.2;

        if (!movingTowardWall && !lookingTowardWall) return null;

        // Mirror the current position across the wall plane and push slightly past the blocks.
        double destinationDistance = -distanceToPlane - side * PHASE_CLEARANCE;
        Vector destination = to.toVector().add(normal.clone().multiply(destinationDistance - distanceToPlane));

        Location arrival = destination.toLocation(player.getWorld());
        arrival.setYaw(to.getYaw());
        arrival.setPitch(to.getPitch());

        if (!isSafe(arrival)) return null;

        lastPhaseAt.put(player.getUniqueId(), now);
        player.setFallDistance(0f);

        arrival.getWorld().spawnParticle(Particle.PORTAL, arrival.clone().add(0, 1, 0), 20, 0.25, 0.5, 0.25, 0.15);
        arrival.getWorld().spawnParticle(Particle.END_ROD, arrival.clone().add(0, 1, 0), 6, 0.15, 0.3, 0.15, 0.02);
        arrival.getWorld().playSound(arrival, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.5f);

        return arrival;
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        return feet.isPassable() && head.isPassable();
    }

    private static Vector normalFor(BlockFace facing) {
        return switch (facing) {
            case NORTH -> new Vector(0, 0, -1);
            case EAST -> new Vector(1, 0, 0);
            case WEST -> new Vector(-1, 0, 0);
            default -> new Vector(0, 0, 1);
        };
    }

    private int[] rotateOffset(int dx, int dz, BlockFace facing) {
        return switch (facing) {
            case SOUTH -> new int[]{dz, dx};
            case NORTH -> new int[]{-dz, -dx};
            case EAST -> new int[]{dx, -dz};
            case WEST -> new int[]{-dx, dz};
            default -> new int[]{dx, dz};
        };
    }

    private boolean isReplaceable(Material material) {
        return material.isAir()
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.SNOW
                || material.name().endsWith("_CARPET");
    }

    @Override
    public boolean tick() {
        if (System.currentTimeMillis() - getPlacedAt() >= durationMillis) {
            return true;
        }

        for (Block block : placedBlocks) {
            Location center = block.getLocation().add(0.5, 0.5, 0.5);
            block.getWorld().spawnParticle(Particle.END_ROD, center, 1, 0.15, 0.2, 0.15, 0.0);
        }

        return false;
    }

    @Override
    public void remove() {
        for (Block block : placedBlocks) {
            StructureBlockRegistry.release(block, this);
        }
        StructureBlockRegistry.releaseAll(this);
        placedBlocks.clear();
        lastPhaseAt.clear();

        if (getLocation().getWorld() != null) {
            getLocation().getWorld().spawnParticle(Particle.POOF,
                    getLocation().clone().add(0.5, height / 2.0, 0.5), 20, 0.5, height / 2.0, 0.5, 0.02);
            getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.4f);
        }
    }

    @Override
    public boolean registerHit(Player attacker) {
        if (!canHit(attacker)) return false;

        remainingHits--;
        getLocation().getWorld().spawnParticle(Particle.CRIT, getLocation().clone().add(0.5, 1.5, 0.5), 10);
        getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_GLASS_HIT, 1f, 1f);

        return remainingHits <= 0;
    }

    @Override
    public boolean canHit(Player attacker) {
        Player owner = getOwner();
        if (owner != null && !teamManager.isEnemy(owner, attacker)) {
            return false;
        }
        return true;
    }

    @Override
    public int getHealthPercentage() {
        return (int) Math.ceil(((double) remainingHits / meleeHitsToDestroy) * 100);
    }

    @Override
    public String getStructureName() {
        return "Phase Wall";
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return placedBlocks;
    }

    public List<Block> getPlacedBlocks() {
        return placedBlocks;
    }
}
