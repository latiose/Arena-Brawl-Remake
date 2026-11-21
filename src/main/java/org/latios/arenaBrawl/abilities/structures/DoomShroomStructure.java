package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.*;

/**
 * The evil counterpart of the {@link TreeOfLifeStructure}: a mushroom that
 * grows over a few seconds, damages every enemy around it in quick pulses and
 * finally explodes for a big burst. Like the tree, it can be chopped down with
 * melee hits before it goes off.
 */
public class DoomShroomStructure extends PlacedStructure implements DestructibleStructure {

    private final double damageRadius;
    private final double damagePerSecond;
    private final int damageIntervalTicks;
    private final double finalBurstDamage;
    private final long totalLifetimeMillis;
    private final long growthDurationMillis;
    private final int meleeHitsToDestroy;

    private final List<StructureBlueprint> growthPhases;
    private final List<Block> placedBlocks = new ArrayList<>();
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Location base;

    private BukkitTask damageTask;
    private int currentPhase = -1;
    private int remainingHits;
    private boolean finished = false;

    public DoomShroomStructure(Player owner, Location location, List<StructureBlueprint> growthPhases,
                               TeamManager teamManager, CombatService combatService,
                               double damagePerSecond, int damageIntervalTicks, double finalBurstDamage,
                               int durationSeconds, int growthSeconds, double damageRadius, int hitsToDestroy) {
        super(owner, location.getBlock().getLocation());
        this.base = location.getBlock().getLocation();
        this.growthPhases = growthPhases;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.damagePerSecond = damagePerSecond;
        this.damageIntervalTicks = Math.max(2, damageIntervalTicks);
        this.finalBurstDamage = finalBurstDamage;
        this.totalLifetimeMillis = Math.max(1, durationSeconds) * 1000L;
        this.growthDurationMillis = Math.max(1, Math.min(growthSeconds * 1000L, this.totalLifetimeMillis));
        this.damageRadius = damageRadius;
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;

        advanceToPhase(0);
        startDamageTask();
    }

    private void startDamageTask() {
        double damagePerPulse = damagePerSecond * (damageIntervalTicks / 20.0);

        damageTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (finished) {
                    cancel();
                    return;
                }
                damageNearbyEnemies(damagePerPulse);
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), damageIntervalTicks, damageIntervalTicks);
    }

    public boolean registerHit(Player attacker) {
        if (!canHit(attacker)) return false;

        remainingHits--;
        base.getWorld().spawnParticle(Particle.CRIT, base.clone().add(0.5, 2, 0.5), 10);
        base.getWorld().playSound(base, Sound.BLOCK_WOOD_HIT, 1f, 0.8f);

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

        long capped = Math.min(elapsed, growthDurationMillis);
        int expectedPhase = (int) Math.min(growthPhases.size() - 1L,
                capped * (long) growthPhases.size() / growthDurationMillis);

        if (expectedPhase > currentPhase) {
            advanceToPhase(expectedPhase);
        } else {
            // Retry cells that were blocked when this phase first grew.
            placePhaseBlocks(currentPhase);
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

        base.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, base.clone().add(0.5, 1.5, 0.5), 12, 0.4, 0.5, 0.4, 0.0);
        base.getWorld().spawnParticle(Particle.SMOKE, base.clone().add(0.5, 1.5, 0.5), 6, 0.3, 0.4, 0.3, 0.01);
        base.getWorld().playSound(base, Sound.BLOCK_WOOD_BREAK, 0.8f, 0.7f);
    }

    private void placePhaseBlocks(int phaseIndex) {
        if (phaseIndex < 0 || phaseIndex >= growthPhases.size()) return;

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

    private boolean isReplaceable(Material material) {
        return material.isAir()
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.SNOW;
    }

    private void damageNearbyEnemies(double amount) {
        Player owner = getOwner();
        if (owner == null || combatService == null) return;

        Location center = base.clone().add(0.5, 1.0, 0.5);
        List<Player> hit = new ArrayList<>();

        for (Entity nearby : center.getWorld().getNearbyEntities(center, damageRadius, damageRadius, damageRadius)) {
            if (!(nearby instanceof Player victim)) continue;
            if (victim.equals(owner)) continue;
            if (victim.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(owner, victim)) continue;

            combatService.applyAbilityDamage(owner, victim, amount, getStructureName());
            hit.add(victim);
        }

        if (hit.isEmpty()) return;

        center.getWorld().spawnParticle(Particle.SOUL, center, 20, damageRadius * 0.5, 0.6, damageRadius * 0.5, 0.01);
        center.getWorld().spawnParticle(Particle.SMOKE, center, 15, damageRadius * 0.5, 0.6, damageRadius * 0.5, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.6f, 0.6f);
    }

    private void finalBurst() {
        Player owner = getOwner();
        if (owner == null) {
            removeBlocks();
            return;
        }

        Location center = base.clone().add(0.5, 1.0, 0.5);

        for (Entity nearby : center.getWorld().getNearbyEntities(center, damageRadius, damageRadius, damageRadius)) {
            if (!(nearby instanceof Player victim)) continue;
            if (victim.equals(owner)) continue;
            if (victim.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(owner, victim)) continue;

            if (combatService != null) {
                combatService.applyAbilityDamage(owner, victim, finalBurstDamage, getStructureName(), center);
            }
        }

        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 80, 1.2, 1.2, 1.2, 0.05);
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 60, 1.5, 1.2, 1.5, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.7f);

        removeBlocks();
    }

    @Override
    public void remove() {
        if (damageTask != null) {
            damageTask.cancel();
            damageTask = null;
        }
        removeBlocks();
    }

    private void removeBlocks() {
        for (Block block : placedBlocks) {
            StructureBlockRegistry.release(block, this);
        }
        StructureBlockRegistry.releaseAll(this);
        base.getWorld().spawnParticle(Particle.POOF, base.clone().add(0.5, 1.5, 0.5), 25);
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return placedBlocks;
    }

    public int getHealthPercentage() {
        return (int) Math.ceil(((double) remainingHits / meleeHitsToDestroy) * 100);
    }

    @Override
    public String getStructureName() {
        return "Doom Shroom";
    }
}
