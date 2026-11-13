package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CobwebFieldStructure extends PlacedStructure implements DestructibleStructure {

    private static final int RADIUS = 1;
    private static final long VIEWER_REFRESH_MS = 250;

    private final TeamManager teamManager;
    private final long durationMillis;
    private final int meleeHitsToDestroy;

    private final List<Block> placedBlocks = new ArrayList<>();
    private final Set<Long> positions = new HashSet<>();
    private final Set<UUID> viewers = new HashSet<>();

    private long lastViewerUpdate;
    private int remainingHits;

    public CobwebFieldStructure(Player owner, Location center, TeamManager teamManager,
                                long durationMillis, int hitsToDestroy) {
        super(owner, center.getBlock().getLocation());
        this.teamManager = teamManager;
        this.durationMillis = durationMillis;
        this.meleeHitsToDestroy = Math.max(1, hitsToDestroy);
        this.remainingHits = this.meleeHitsToDestroy;

        build(center.getBlock());
        updateViewers();
    }

    private void build(Block center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                Block block = center.getRelative(dx, 0, dz);

                if (!isReplaceable(block.getType())) continue;
                if (StructureBlockRegistry.isOwnedByOther(block, this)) continue;

                StructureBlockRegistry.claim(block, this);
                block.setType(Material.COBWEB, false);
                placedBlocks.add(block);
                positions.add(CobwebVisibilityFilter.pack(block.getX(), block.getY(), block.getZ()));
            }
        }
    }

    private static boolean isReplaceable(Material material) {
        return material.isAir()
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.SNOW
                || material.name().endsWith("_CARPET");
    }


    private void updateViewers() {
        lastViewerUpdate = System.currentTimeMillis();

        Player owner = getOwner();
        World world = getLocation().getWorld();
        if (owner == null || world == null) return;

        Set<UUID> wanted = new HashSet<>();
        for (Player p : world.getPlayers()) {
            if (p.equals(owner) || teamManager.isAlly(owner, p)) {
                wanted.add(p.getUniqueId());
            }
        }

        for (UUID id : wanted) {
            if (viewers.add(id)) {
                CobwebVisibilityFilter.hide(id, positions);
                resync(id);
            }
        }

        Iterator<UUID> it = viewers.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            if (!wanted.contains(id)) {
                it.remove();
                CobwebVisibilityFilter.show(id, positions);
                resync(id);
            }
        }
    }

    private void resync(UUID id) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) return;
        for (Block block : placedBlocks) {
            player.sendBlockChange(block.getLocation(), block.getBlockData());
        }
    }

    @Override
    public boolean tick() {
        if (System.currentTimeMillis() - getPlacedAt() >= durationMillis) {
            return true;
        }
        if (System.currentTimeMillis() - lastViewerUpdate >= VIEWER_REFRESH_MS) {
            updateViewers();
        }
        return false;
    }

    @Override
    public void remove() {
        for (UUID id : viewers) {
            CobwebVisibilityFilter.show(id, positions);
        }
        viewers.clear();

        for (Block block : placedBlocks) {
            StructureBlockRegistry.release(block, this);
        }
        StructureBlockRegistry.releaseAll(this);

        if (getLocation().getWorld() != null) {
            getLocation().getWorld().spawnParticle(Particle.POOF,
                    getLocation().clone().add(0.5, 0.5, 0.5), 20, 1.0, 0.5, 1.0, 0.01);
            getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_COBWEB_BREAK, 1f, 1f);
        }

        placedBlocks.clear();
        positions.clear();
    }

    @Override
    public boolean registerHit(Player attacker) {
        if (!canHit(attacker)) return false;

        remainingHits--;
        getLocation().getWorld().spawnParticle(Particle.CRIT,
                getLocation().clone().add(0.5, 0.5, 0.5), 12, 1.0, 0.5, 1.0, 0.05);
        getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_COBWEB_HIT, 1f, 1f);

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
        return "Cobweb Field";
    }

    @Override
    public List<Block> getOccupiedBlocks() {
        return placedBlocks;
    }

    public List<Block> getPlacedBlocks() {
        return placedBlocks;
    }
}