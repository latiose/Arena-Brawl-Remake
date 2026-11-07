package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.*;

public class TreeOfLifeStructure extends PlacedStructure {

    private static final long DEFAULT_GROWTH_DURATION_MILLIS = 5_000;

    private final double healRadius;
    private final double healPerSecond;
    private final double finalBurstHeal;
    private final long totalLifetimeMillis;
    private final long growthDurationMillis;
    private final int meleeHitsToDestroy;

    private final List<StructureBlueprint> growthPhases;
    private final List<Block> placedBlocks = new ArrayList<>();
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final Location base;

    private int currentPhase = -1;
    private long lastHealTickAt;
    private int remainingHits;
    private boolean finished = false;

    public TreeOfLifeStructure(Player owner, Location location, List<StructureBlueprint> growthPhases,
                               TeamManager teamManager, PlayerHealthManager healthManager,
                               double healPerSecond, double finalBurstHeal, int durationSeconds,
                               double healRadius, int hitsToDestroy) {
        super(owner, location.getBlock().getLocation());
        this.base = location.getBlock().getLocation();
        this.growthPhases = growthPhases;
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.healPerSecond = healPerSecond;
        this.finalBurstHeal = finalBurstHeal;
        this.totalLifetimeMillis = Math.max(1, durationSeconds) * 1000L;
        this.growthDurationMillis = Math.min(DEFAULT_GROWTH_DURATION_MILLIS, this.totalLifetimeMillis);
        this.healRadius = healRadius;
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;
        this.lastHealTickAt = System.currentTimeMillis();

        advanceToPhase(0);
    }

    public boolean registerHit(Player attacker) {
        if(!canHit(attacker)) return false;

        remainingHits--;
        base.getWorld().spawnParticle(Particle.CRIT, base.clone().add(0.5, 2, 0.5), 10);
        base.getWorld().playSound(base, Sound.BLOCK_WOOD_HIT, 1f, 1f);

        return remainingHits <= 0;
    }

    public boolean canHit(Player attacker) {
        Player owner = getOwner();
        if (owner != null && !teamManager.isEnemy(owner, attacker)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean tick() {
        if (finished) return true;

        long elapsed = System.currentTimeMillis() - getPlacedAt();

        int expectedPhase = Math.min(
                growthPhases.size() - 1,
                (int) (Math.min(elapsed, growthDurationMillis) / 1000)
        );
        if (expectedPhase > currentPhase) {
            advanceToPhase(expectedPhase);
        } else {
            // Retry cells that were blocked when this phase first grew (for example
            // by a Barricade), so the tree fills in once they become available.
            placePhaseBlocks(currentPhase);
        }

        if (System.currentTimeMillis() - lastHealTickAt >= 1000) {
            lastHealTickAt = System.currentTimeMillis();
            healNearbyAllies(healPerSecond, false);
        }

        if (elapsed >= totalLifetimeMillis) {
            finalBurst();
            finished = true;
            return true;
        }

        return false;
    }

    private void advanceToPhase(int phaseIndex) {
        placePhaseBlocks(phaseIndex);
        currentPhase = phaseIndex;
        base.getWorld().spawnParticle(Particle.COMPOSTER, base.clone().add(0.5, 2, 0.5), 15);
    }

    private void placePhaseBlocks(int phaseIndex) {
        StructureBlueprint blueprint = growthPhases.get(phaseIndex);

        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            Block block = base.clone().add(offset.dx(), offset.dy(), offset.dz()).getBlock();

            if (placedBlocks.contains(block)) continue;

            if (!isReplaceable(block.getType())) continue;
            if (StructureBlockRegistry.isOwnedByOther(block, this)) continue;

            StructureBlockRegistry.claim(block, this);
            block.setType(offset.material());
            placedBlocks.add(block);
        }
    }

    public int getHealthPercentage() {
        return (int) Math.ceil(((double) remainingHits / meleeHitsToDestroy) * 100);
    }

    private boolean isReplaceable(org.bukkit.Material material) {
        return material.isAir()
                || material == org.bukkit.Material.SHORT_GRASS
                || material == org.bukkit.Material.TALL_GRASS;
    }

    private void healNearbyAllies(double amount, boolean isBurst) {
        Player owner = getOwner();
        if (owner == null) return;

        Location center = base.clone().add(0.5, 1, 0.5);
        Set<Player> healed = new HashSet<>();

        for (Entity nearby : center.getWorld().getNearbyEntities(center, healRadius, healRadius, healRadius)) {
            if (nearby instanceof Player candidate
                    && (candidate.equals(owner) || teamManager.isAlly(owner, candidate))
                    && !healed.contains(candidate)) {
                if (candidate.equals(owner)) {
                    healthManager.heal(owner, amount, "Tree of life");
                } else {
                    healthManager.healAlly(owner, candidate, amount, "Tree of life");
                }
                healed.add(candidate);
            }
        }

        if (healed.isEmpty()) return;

        if (isBurst) {
            center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 60, 1, 1.5, 1, 0.1);
            center.getWorld().playSound(center, Sound.ITEM_TOTEM_USE, 5f, 1f);
        } else {
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 10, 1, 1, 1);
        }
    }

    private void finalBurst() {
        healNearbyAllies(finalBurstHeal, true);
        removeBlocks();
    }

    @Override
    public void remove() {
        removeBlocks();
    }

    private void removeBlocks() {
        for (Block block : placedBlocks) {
            StructureBlockRegistry.release(block, this);
        }
        StructureBlockRegistry.releaseAll(this);
        base.getWorld().spawnParticle(Particle.POOF, base.clone().add(0.5, 2, 0.5), 25);
    }

    public List<Block> getOccupiedBlocks() {
        return placedBlocks;
    }
}