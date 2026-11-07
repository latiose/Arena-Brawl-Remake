package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Candle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MagicTableStructure extends PlacedStructure {

    private final double healRadius;
    private final double healAmount;
    private final long healDelayMillis;
    private final int meleeHitsToDestroy;

    private final List<Block> structureBlocks = new ArrayList<>();
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    private final long createdAt;
    private int remainingHits;

    public MagicTableStructure(Player owner, Location location, TeamManager teamManager, PlayerHealthManager healthManager,
                               double healAmount, double healRadius, int hitsToDestroy, long healDelayMillis) {
        super(owner, location.getBlock().getLocation());
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.healAmount = healAmount;
        this.healRadius = healRadius;
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;
        this.healDelayMillis = healDelayMillis;
        this.createdAt = System.currentTimeMillis();
        buildStructure(location.getBlock().getLocation());
    }

    private void buildStructure(Location base) {
        // Bloque inferior: Mesa de Encantamientos
        Block baseBlock = base.clone().add(0, 0, 0).getBlock();
        StructureBlockRegistry.claim(baseBlock, this);
        baseBlock.setType(Material.ENCHANTING_TABLE);
        structureBlocks.add(baseBlock);

        // Bloque superior: Vela encendida
        Block topBlock = base.clone().add(0, 1, 0).getBlock();
        StructureBlockRegistry.claim(topBlock, this);
        topBlock.setType(Material.CANDLE);
        if (topBlock.getBlockData() instanceof Candle candle) {
            candle.setLit(true);
            topBlock.setBlockData(candle);
        }
        structureBlocks.add(topBlock);
    }

    public boolean registerHit(Player attacker) {
        if (!canHit(attacker)) return false;

        remainingHits--;
        getLocation().getWorld().spawnParticle(Particle.CRIT, getLocation().clone().add(0.5, 1, 0.5), 10);
        getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_WOOD_HIT, 1f, 1f);

        return remainingHits <= 0;
    }

    public boolean canHit(Player attacker) {
        Player owner = Bukkit.getPlayer(getOwnerId());
        return owner == null || teamManager.isEnemy(owner, attacker);
    }

    @Override
    public boolean tick() {
        if (System.currentTimeMillis() - createdAt < healDelayMillis) {
            return false;
        }

        Player owner = Bukkit.getPlayer(getOwnerId());
        if (owner == null) return true;

        Set<Player> healed = new HashSet<>();
        Location center = getLocation().clone().add(0.5, 1, 0.5);

        for (Entity nearby : center.getWorld().getNearbyEntities(center, healRadius, healRadius, healRadius)) {
            if (nearby instanceof Player candidate
                    && (candidate.equals(owner) || teamManager.isAlly(owner, candidate))
                    && !healed.contains(candidate)) {
                if (candidate.equals(owner)) {
                    healthManager.heal(owner, healAmount, "Magic Table");
                } else {
                    healthManager.healAlly(owner, candidate, healAmount, "Magic Table");
                }
                healed.add(candidate);
            }
        }

        if (!healed.isEmpty()) {
            center.getWorld().spawnParticle(Particle.HEART, center, 20, 0.8, 0.8, 0.8, 0.1);
            center.getWorld().spawnParticle(Particle.END_ROD, center, 30, 0.5, 0.5, 0.5, 0.05);
            center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 5f, 1.2f);
            center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 5, 0.8f);
        }

        return true;
    }

    @Override
    public void remove() {
        for (Block block : structureBlocks) {
            StructureBlockRegistry.release(block, this);
        }
        StructureBlockRegistry.releaseAll(this);
        getLocation().getWorld().spawnParticle(Particle.ENCHANT, getLocation().clone().add(0.5, 1, 0.5), 25, 0.3, 0.3, 0.3, 0.5);
    }

    public List<Block> getStructureBlocks() {
        return structureBlocks;
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return structureBlocks;
    }

    public int getHealthPercentage() {
        return (int) Math.ceil(((double) remainingHits / meleeHitsToDestroy) * 100);
    }
}