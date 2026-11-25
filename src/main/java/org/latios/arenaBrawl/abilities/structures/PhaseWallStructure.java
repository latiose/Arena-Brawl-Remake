package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhaseWallStructure extends PlacedStructure implements DestructibleStructure {

    private static final double PHASE_DISTANCE = 1.15;
    private static final double PHASE_CLEARANCE = 0.75;
    private static final long PHASE_COOLDOWN_MILLIS = 500L;

    private final TeamManager teamManager;
    private final long durationMillis;
    private final int meleeHitsToDestroy;

    private final List<Block> placedBlocks = new ArrayList<>();
    private final Map<UUID, Long> lastPhaseAt = new HashMap<>();
    private final List<WallPlane> walls = new ArrayList<>();

    private final Location center;
    private final double height;

    private int remainingHits;
    private int tickCounter;

    private record WallPlane(Vector normal, Vector widthAxis, Location center, double halfWidth) {}

    public PhaseWallStructure(Player owner, Location base, StructureBlueprint blueprint,
                              TeamManager teamManager, long durationMillis, int hitsToDestroy) {
        super(owner, base.getBlock().getLocation());
        this.teamManager = teamManager;
        this.durationMillis = durationMillis;
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int maxY = 0;

        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            minX = Math.min(minX, offset.dx());
            maxX = Math.max(maxX, offset.dx());
            minZ = Math.min(minZ, offset.dz());
            maxZ = Math.max(maxZ, offset.dz());
            maxY = Math.max(maxY, offset.dy());
        }

        Location origin = base.getBlock().getLocation();
        this.height = maxY + 1;

        double centerX = origin.getBlockX() + (minX + maxX + 1) / 2.0;
        double centerZ = origin.getBlockZ() + (minZ + maxZ + 1) / 2.0;
        double centerY = origin.getBlockY() + height / 2.0;
        this.center = new Location(origin.getWorld(), centerX, centerY, centerZ);

        double halfWidthX = (maxX - minX + 1) / 2.0;
        double halfWidthZ = (maxZ - minZ + 1) / 2.0;

        walls.add(new WallPlane(new Vector(-1, 0, 0), new Vector(0, 0, 1),
                new Location(origin.getWorld(), origin.getBlockX() + minX + 0.5, centerY, centerZ),
                halfWidthZ));
        walls.add(new WallPlane(new Vector(1, 0, 0), new Vector(0, 0, 1),
                new Location(origin.getWorld(), origin.getBlockX() + maxX + 0.5, centerY, centerZ),
                halfWidthZ));
        walls.add(new WallPlane(new Vector(0, 0, -1), new Vector(1, 0, 0),
                new Location(origin.getWorld(), centerX, centerY, origin.getBlockZ() + minZ + 0.5),
                halfWidthX));
        walls.add(new WallPlane(new Vector(0, 0, 1), new Vector(1, 0, 0),
                new Location(origin.getWorld(), centerX, centerY, origin.getBlockZ() + maxZ + 0.5),
                halfWidthX));

        build(origin, blueprint);
    }

    private void build(Location origin, StructureBlueprint blueprint) {
        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            Block block = origin.clone().add(offset.dx(), offset.dy(), offset.dz()).getBlock();

            if (!isReplaceable(block.getType())) continue;
            if (StructureBlockRegistry.isOwnedByOther(block, this)) continue;

            StructureBlockRegistry.claim(block, this);
            block.setType(offset.material());
            placedBlocks.add(block);
        }
    }

    public Location resolvePhase(Player player, Location from, Location to) {
        Player owner = getOwner();
        if (owner == null) return null;
        if (!player.equals(owner) && !teamManager.isAlly(owner, player)) return null;

        long now = System.currentTimeMillis();
        if (now - lastPhaseAt.getOrDefault(player.getUniqueId(), 0L) < PHASE_COOLDOWN_MILLIS) {
            return null;
        }

        WallPlane target = null;
        double targetSigned = 0;
        double targetAbs = Double.MAX_VALUE;

        for (WallPlane wall : walls) {
            Vector delta = to.toVector().subtract(wall.center().toVector());
            double signed = delta.dot(wall.normal());
            if (Math.abs(signed) > PHASE_DISTANCE) continue;

            double lateral = delta.dot(wall.widthAxis());
            if (Math.abs(lateral) > wall.halfWidth()) continue;

            double vertical = delta.getY();
            if (vertical < -height / 2.0 - 0.6 || vertical > height / 2.0 + 0.6) continue;

            double abs = Math.abs(signed);
            if (abs < targetAbs) {
                targetAbs = abs;
                targetSigned = signed;
                target = wall;
            }
        }

        if (target == null) return null;

        double side = targetSigned >= 0 ? 1 : -1;

        Vector movement = to.toVector().subtract(from.toVector());
        boolean movingTowardWall = movement.dot(target.normal()) * side < -0.001;

        Vector look = player.getEyeLocation().getDirection();
        look.setY(0);
        if (look.lengthSquared() < 1.0E-6) return null;
        look.normalize();
        boolean lookingTowardWall = look.dot(target.normal()) * side < -0.2;

        if (!movingTowardWall && !lookingTowardWall) return null;

        double destinationDistance = -targetSigned - side * PHASE_CLEARANCE;
        Vector destination = to.toVector().add(
                target.normal().clone().multiply(destinationDistance - targetSigned));

        Location arrival = destination.toLocation(player.getWorld());
        arrival.setYaw(to.getYaw());
        arrival.setPitch(to.getPitch());

        if (!isSafe(arrival)) return null;

        lastPhaseAt.put(player.getUniqueId(), now);
        player.setFallDistance(0f);

        arrival.getWorld().spawnParticle(Particle.PORTAL, arrival.clone().add(0, 1, 0),
                20, 0.25, 0.5, 0.25, 0.15);
        arrival.getWorld().spawnParticle(Particle.END_ROD, arrival.clone().add(0, 1, 0),
                6, 0.15, 0.3, 0.15, 0.02);
        arrival.getWorld().playSound(arrival, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.5f);

        return arrival;
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        return feet.isPassable() && head.isPassable();
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

        tickCounter++;

        for (int i = 0; i < placedBlocks.size(); i++) {
            if ((i + tickCounter) % 4 != 0) continue;

            Block block = placedBlocks.get(i);
            Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
            block.getWorld().spawnParticle(Particle.END_ROD, blockCenter, 1, 0.15, 0.2, 0.15, 0.0);
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
        walls.clear();

        if (center.getWorld() != null) {
            center.getWorld().spawnParticle(Particle.POOF, center, 40, 1.2, height / 2.0, 1.2, 0.02);
            center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.4f);
        }
    }

    @Override
    public boolean registerHit(Player attacker) {
        if (!canHit(attacker)) return false;

        remainingHits--;
        center.getWorld().spawnParticle(Particle.CRIT, center, 16, 0.8, 0.8, 0.8, 0.05);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_HIT, 1f, 1f);

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
