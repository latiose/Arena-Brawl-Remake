package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.*;

public class TreeOfLifeStructure extends PlacedStructure {

    private static final double HEAL_RADIUS = 5.0;
    private static final double HEAL_PER_SECOND = 50.0;
    private static final double FINAL_BURST_HEAL = 400.0;
    private static final long TOTAL_LIFETIME_MILLIS = 7_000;
    private static final long GROWTH_DURATION_MILLIS = 5_000;
    private static final int MELEE_HITS_TO_DESTROY = 8;

    private final List<StructureBlueprint> growthPhases;
    private final Map<Block, BlockData> originalBlockData = new HashMap<>();
    private final List<Block> placedBlocks = new ArrayList<>();
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final Location base;

    private int currentPhase = -1;
    private long lastHealTickAt;
    private int remainingHits = MELEE_HITS_TO_DESTROY;
    private boolean finished = false;

    public TreeOfLifeStructure(Player owner, Location location, List<StructureBlueprint> growthPhases,
                               TeamManager teamManager, PlayerHealthManager healthManager) {
        super(owner, location.getBlock().getLocation());
        this.base = location.getBlock().getLocation();
        this.growthPhases = growthPhases;
        this.teamManager = teamManager;
        this.healthManager = healthManager;
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
                (int) (Math.min(elapsed, GROWTH_DURATION_MILLIS) / 1000)
        );
        if (expectedPhase > currentPhase) {
            advanceToPhase(expectedPhase);
        }

        if (System.currentTimeMillis() - lastHealTickAt >= 1000) {
            lastHealTickAt = System.currentTimeMillis();
            healNearbyAllies(HEAL_PER_SECOND, false);
        }

        if (elapsed >= TOTAL_LIFETIME_MILLIS) {
            finalBurst();
            finished = true;
            return true;
        }

        return false;
    }

    private void advanceToPhase(int phaseIndex) {
        StructureBlueprint blueprint = growthPhases.get(phaseIndex);

        for (StructureBlueprint.BlockOffset offset : blueprint.getOffsets()) {
            Block block = base.clone().add(offset.dx(), offset.dy(), offset.dz()).getBlock();

            if (placedBlocks.contains(block)) continue;

            if (!isReplaceable(block.getType())) continue;

            originalBlockData.put(block, block.getBlockData());
            block.setType(offset.material());
            placedBlocks.add(block);
        }

        currentPhase = phaseIndex;
        base.getWorld().spawnParticle(Particle.COMPOSTER, base.clone().add(0.5, 2, 0.5), 15);
    }

    public int getHealthPercentage() {
        return (int) Math.ceil(((double) remainingHits / MELEE_HITS_TO_DESTROY) * 100);
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

        for (Entity nearby : center.getWorld().getNearbyEntities(center, HEAL_RADIUS, HEAL_RADIUS, HEAL_RADIUS)) {
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
            center.getWorld().playSound(center, Sound.ITEM_TOTEM_USE, 1f, 1f);
        } else {
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 10, 1, 1, 1);
        }
    }

    private void finalBurst() {
        healNearbyAllies(FINAL_BURST_HEAL, true);
        removeBlocks();
    }

    @Override
    public void remove() {
        removeBlocks();
    }

    private void removeBlocks() {
        for (Block block : placedBlocks) {
            BlockData original = originalBlockData.get(block);
            if (original != null) {
                block.setBlockData(original);
            } else {
                block.setType(org.bukkit.Material.AIR);
            }
        }
        base.getWorld().spawnParticle(Particle.POOF, base.clone().add(0.5, 2, 0.5), 25);
    }

    public List<Block> getOccupiedBlocks() {
        return placedBlocks;
    }
}