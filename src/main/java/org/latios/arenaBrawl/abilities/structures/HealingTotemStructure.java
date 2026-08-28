package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HealingTotemStructure extends PlacedStructure {

    private static final double HEAL_RADIUS = 4.0;
    private static final double HEAL_AMOUNT = 300.0;
    private static final int MAX_HEAL_PULSES = 3;
    private static final long HEAL_INTERVAL_MILLIS = 3_000;
    private static final int MELEE_HITS_TO_DESTROY = 4;

    private final List<Block> standBlocks = new ArrayList<>();
    private final List<BlockData> originalBlockData = new ArrayList<>();
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    private int healPulsesUsed = 0;
    private long lastHealAt;
    private int remainingHits = MELEE_HITS_TO_DESTROY;

    public HealingTotemStructure(Player owner, Location location, TeamManager teamManager, PlayerHealthManager healthManager) {
        super(owner, location.getBlock().getLocation());
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.lastHealAt = System.currentTimeMillis();
        buildStands(location.getBlock().getLocation());
    }

    private void buildStands(Location base) {
        for (int i = 0; i < 2; i++) {
            Block block = base.clone().add(0, i, 0).getBlock();
            originalBlockData.add(block.getBlockData());
            block.setType(org.bukkit.Material.BREWING_STAND);
            standBlocks.add(block);
        }
    }

    /** Registers a hit from an enemy. Returns true if the totem is destroyed by this hit. */
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
        if (healPulsesUsed >= MAX_HEAL_PULSES) {
            return true;
        }

        if (System.currentTimeMillis() - lastHealAt < HEAL_INTERVAL_MILLIS) {
            return false;
        }

        lastHealAt = System.currentTimeMillis();
        healPulsesUsed++;

        Player owner = org.bukkit.Bukkit.getPlayer(getOwnerId());
        if (owner == null) return healPulsesUsed >= MAX_HEAL_PULSES;

        Set<Player> healed = new HashSet<>();
        Location center = getLocation().clone().add(0.5, 1, 0.5);

        for (Entity nearby : center.getWorld().getNearbyEntities(center, HEAL_RADIUS, HEAL_RADIUS, HEAL_RADIUS)) {
            if (nearby instanceof Player candidate
                    && (candidate.equals(owner) || teamManager.isAlly(owner, candidate))
                    && !healed.contains(candidate)) {
                healthManager.heal(candidate, HEAL_AMOUNT);
                healed.add(candidate);
            }
        }

        if (!healed.isEmpty()) {
            center.getWorld().spawnParticle(Particle.FIREWORK, center, 40, 0.5, 1, 0.5, 0.05);
            center.getWorld().playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1f);

            int healInt = (int) HEAL_AMOUNT;

            for (Player p : healed) {
                if (p.equals(owner)) {
                    owner.sendMessage(MessageUtils.positive() + String.format("§3Your Healing Totem healed you for §a%d §3health!", healInt));
                } else {
                    p.sendMessage(MessageUtils.positive() + String.format("§a%s§3's Healing Totem healed you for §a%d §3health!", owner.getName(), healInt));
                    owner.sendMessage(MessageUtils.positive() + String.format("§3Your Healing Totem healed §a%s §3for §a%d §3health!", p.getName(), healInt));
                }
            }
        }

        return healPulsesUsed >= MAX_HEAL_PULSES;
    }

    @Override
    public void remove() {
        for (int i = 0; i < standBlocks.size(); i++) {
            standBlocks.get(i).setBlockData(originalBlockData.get(i));
        }
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
        return (int) Math.ceil(((double) remainingHits / MELEE_HITS_TO_DESTROY) * 100);
    }
}