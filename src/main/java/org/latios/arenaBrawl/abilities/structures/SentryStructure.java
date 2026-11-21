package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A small turret that fires at the nearest visible enemy. It is made of two real
 * blocks so enemies can chop it down with melee hits.
 */
public class SentryStructure extends PlacedStructure implements DestructibleStructure {

    private final TeamManager teamManager;
    private final CombatService combatService;
    private final MatchManager matchManager;
    private final double damage;
    private final double range;
    private final int fireIntervalTicks;
    private final long lifetimeMillis;
    private final int meleeHitsToDestroy;

    private final List<Block> placedBlocks = new ArrayList<>();

    private int remainingHits;
    private BukkitTask fireTask;

    public SentryStructure(Player owner, Location base, TeamManager teamManager, CombatService combatService,
                           MatchManager matchManager, double damage, double range, int fireIntervalTicks,
                           int lifetimeSeconds, int hitsToDestroy) {
        super(owner, base.getBlock().getLocation());
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.matchManager = matchManager;
        this.damage = damage;
        this.range = range;
        this.fireIntervalTicks = Math.max(2, fireIntervalTicks);
        this.lifetimeMillis = Math.max(1, lifetimeSeconds) * 1000L;
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;

        build();
        startFiring();
    }

    private void build() {
        Block base = getLocation().getBlock();
        Block head = base.getRelative(0, 1, 0);

        place(base, Material.IRON_BLOCK);
        place(head, Material.TARGET);

        getLocation().getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                getLocation().clone().add(0.5, 1.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
        getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.7f, 1.6f);
    }

    private void place(Block block, Material material) {
        if (!isReplaceable(block.getType())) return;
        if (StructureBlockRegistry.isOwnedByOther(block, this)) return;

        StructureBlockRegistry.claim(block, this);
        block.setType(material);
        placedBlocks.add(block);
    }

    private boolean isReplaceable(Material material) {
        return material.isAir()
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.SNOW
                || material.name().endsWith("_CARPET");
    }

    private void startFiring() {
        fireTask = new BukkitRunnable() {
            @Override
            public void run() {
                fire();
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), fireIntervalTicks, fireIntervalTicks);
    }

    private void fire() {
        Player owner = getOwner();
        if (owner == null || !owner.isOnline() || owner.isDead()
                || owner.getGameMode() == GameMode.SPECTATOR
                || (matchManager != null && !matchManager.isInMatch(owner))) {
            return;
        }

        Shot shot = findShot(owner);
        if (shot == null) return;

        combatService.applyAbilityDamage(owner, shot.target(), damage, getStructureName(), shot.impact());

        drawBeam(shot.origin(), shot.impact());
        shot.impact().getWorld().spawnParticle(Particle.CRIT, shot.impact(), 6, 0.2, 0.3, 0.2, 0.05);
        shot.impact().getWorld().playSound(shot.impact(), Sound.ENTITY_ARROW_HIT, 0.5f, 1.6f);
    }

    private record Shot(Player target, Location origin, Location impact) {
    }

    private Shot findShot(Player owner) {
        if (matchManager == null) return null;

        Location core = getLocation().clone().add(0.5, 1.5, 0.5);
        double rangeSquared = range * range;
        double bestDistance = Double.MAX_VALUE;
        Shot best = null;

        for (Player candidate : matchManager.getMatchPlayers(owner)) {
            if (candidate.equals(owner) || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(owner, candidate)) continue;

            Location targetLoc = candidate.getLocation().add(0, 1.0, 0);
            Location origin = firingOrigin(core, targetLoc);
            double distance = origin.distanceSquared(targetLoc);
            if (distance > rangeSquared || distance >= bestDistance) continue;

            Vector direction = targetLoc.toVector().subtract(origin.toVector());
            double length = direction.length();
            if (length < 0.1) continue;

            RayTraceResult blockHit = origin.getWorld().rayTraceBlocks(
                    origin, direction.normalize(), length, FluidCollisionMode.NEVER, true);
            if (blockHit != null) continue;

            bestDistance = distance;
            best = new Shot(candidate, origin, targetLoc);
        }

        return best;
    }

    /**
     * Starts the shot just outside the turret's own head block. Firing from the block
     * center would make the turret's own target block intercept every line of sight.
     */
    private Location firingOrigin(Location core, Location targetLoc) {
        Vector horizontal = new Vector(targetLoc.getX() - core.getX(), 0, targetLoc.getZ() - core.getZ());

        if (horizontal.lengthSquared() < 0.01) {
            return core.clone().add(0, 0.9, 0);
        }

        return core.clone().add(horizontal.normalize().multiply(0.9)).add(0, 0.2, 0);
    }

    private void drawBeam(Location from, Location to) {
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        if (length < 0.1) return;

        Vector step = delta.clone().normalize().multiply(0.4);
        Location point = from.clone();

        for (double travelled = 0; travelled < length; travelled += 0.4) {
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);
            point.add(step);
        }
    }

    @Override
    public boolean tick() {
        return System.currentTimeMillis() - getPlacedAt() >= lifetimeMillis;
    }

    @Override
    public void remove() {
        if (fireTask != null) {
            fireTask.cancel();
            fireTask = null;
        }

        for (Block block : placedBlocks) {
            StructureBlockRegistry.release(block, this);
        }
        StructureBlockRegistry.releaseAll(this);
        placedBlocks.clear();

        getLocation().getWorld().spawnParticle(Particle.POOF,
                getLocation().clone().add(0.5, 0.8, 0.5), 12, 0.2, 0.3, 0.2, 0.02);
        getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_METAL_BREAK, 0.7f, 1.2f);
    }

    @Override
    public boolean registerHit(Player attacker) {
        if (!canHit(attacker)) return false;

        remainingHits--;
        getLocation().getWorld().spawnParticle(Particle.CRIT,
                getLocation().clone().add(0.5, 1.0, 0.5), 8);
        getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_METAL_HIT, 0.8f, 1.2f);

        return remainingHits <= 0;
    }

    @Override
    public boolean canHit(Player attacker) {
        Player owner = getOwner();
        return owner == null || teamManager.isEnemy(owner, attacker);
    }

    @Override
    public int getHealthPercentage() {
        return (int) Math.ceil(((double) remainingHits / meleeHitsToDestroy) * 100);
    }

    @Override
    public String getStructureName() {
        return "Sentry";
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return placedBlocks;
    }
}
