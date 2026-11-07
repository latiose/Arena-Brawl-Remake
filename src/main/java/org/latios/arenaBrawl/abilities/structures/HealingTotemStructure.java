package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HealingTotemStructure extends PlacedStructure {

    private static final long HEAL_INTERVAL_MILLIS = 3_000;

    private final double healRadius;
    private final double healAmount;
    private final int maxHealPulses;
    private final int meleeHitsToDestroy;

    private final List<Block> standBlocks = new ArrayList<>();
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    private int healPulsesUsed = 0;
    private long lastHealAt;
    private int remainingHits;

    public HealingTotemStructure(Player owner, Location location, TeamManager teamManager, PlayerHealthManager healthManager,
                                 double healAmount, double healRadius, int pulses, int hitsToDestroy) {
        super(owner, location.getBlock().getLocation());
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.healAmount = healAmount;
        this.healRadius = healRadius;
        this.maxHealPulses = Math.max(1, pulses);
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;
        this.lastHealAt = System.currentTimeMillis();
        buildStands(location.getBlock().getLocation());
    }

    private void buildStands(Location base) {
        Block baseBlock = base.clone().getBlock();
        StructureBlockRegistry.claim(baseBlock, this);
        baseBlock.setType(org.bukkit.Material.BREWING_STAND);
        standBlocks.add(baseBlock);

        Block topBlock = base.clone().add(0, 1, 0).getBlock();
        StructureBlockRegistry.claim(topBlock, this);
        topBlock.setType(org.bukkit.Material.BREWING_STAND);
        standBlocks.add(topBlock);
    }

    public boolean registerHit(Player attacker) {
        if(!canHit(attacker)) return false;

        remainingHits--;
        getLocation().getWorld().spawnParticle(Particle.CRIT, getLocation().clone().add(0.5, 1, 0.5), 10);
        getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_WOOD_HIT, 1f, 1f);

        return remainingHits <= 0;
    }

    public boolean canHit(Player attacker){
        Player owner = org.bukkit.Bukkit.getPlayer(getOwnerId());
        if (owner != null && !teamManager.isEnemy(owner, attacker)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean tick() {
        if (healPulsesUsed >= maxHealPulses) {
            return true;
        }

        if (System.currentTimeMillis() - lastHealAt < HEAL_INTERVAL_MILLIS) {
            return false;
        }

        lastHealAt = System.currentTimeMillis();
        healPulsesUsed++;

        Player owner = org.bukkit.Bukkit.getPlayer(getOwnerId());
        if (owner == null) return healPulsesUsed >= maxHealPulses;

        Set<Player> healed = new HashSet<>();
        Location center = getLocation().clone().add(0.5, 1, 0.5);

        for (Entity nearby : center.getWorld().getNearbyEntities(center, healRadius, healRadius, healRadius)) {
            if (nearby instanceof Player candidate
                    && (candidate.equals(owner) || teamManager.isAlly(owner, candidate))
                    && !healed.contains(candidate)) {
                if (candidate.equals(owner)) {
                    healthManager.heal(owner, healAmount, "Healing Totem");
                } else {
                    healthManager.healAlly(owner, candidate, healAmount, "Healing Totem");
                }
                healed.add(candidate);
            }
        }

        if (!healed.isEmpty()) {
            center.getWorld().spawnParticle(Particle.FIREWORK, center, 40, 0.5, 1, 0.5, 0.05);
            center.getWorld().playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 5f, 1f);
        }

        return healPulsesUsed >= maxHealPulses;
    }

    @Override
    public void remove() {
        for (Block block : standBlocks) {
            StructureBlockRegistry.release(block, this);
        }
        StructureBlockRegistry.releaseAll(this);
        getLocation().getWorld().spawnParticle(Particle.FIREWORK, getLocation().clone().add(0.5, 1, 0.5), 15);
    }

    public List<Block> getStandBlocks() {
        return standBlocks;
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return standBlocks;
    }

    public int getHealthPercentage() {
        return (int) Math.ceil(((double) remainingHits / meleeHitsToDestroy) * 100);
    }
}