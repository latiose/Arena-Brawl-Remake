
package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

public class StructureDemolitionService {

    private final StructureManager structureManager;
    private final TeamManager teamManager;

    public StructureDemolitionService(StructureManager structureManager, TeamManager teamManager) {
        this.structureManager = structureManager;
        this.teamManager = teamManager;
    }

    /** Finds the nearest structure with a block within `radius` of the given point, or null if none. */
    public PlacedStructure findStructureNear(Location point, double radius) {
        for (PlacedStructure structure : new ArrayList<>(structureManager.getAll())) {
            for (Block block : structure.getOccupiedBlocks()) {
                Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
                if (blockCenter.distance(point) <= radius) {
                    return structure;
                }
            }
        }
        return null;
    }

    /** Finds every structure with any block within `radius` of the given point (for AoE effects like GolemFall). */
    public List<PlacedStructure> findStructuresInRadius(Location center, double radius) {
        List<PlacedStructure> found = new ArrayList<>();
        for (PlacedStructure structure : new ArrayList<>(structureManager.getAll())) {
            for (Block block : structure.getOccupiedBlocks()) {
                Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
                if (blockCenter.distance(center) <= radius) {
                    found.add(structure);
                    break;
                }
            }
        }
        return found;
    }

    public boolean isBlockPartOfAnyStructure(Block block) {
        for (PlacedStructure structure : structureManager.getAll()) {
            if (structure.getOccupiedBlocks().contains(block)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if the structure belongs to an enemy of `actor` (or has no valid owner, treated as breakable). */
    public boolean isEnemyStructure(Player actor, PlacedStructure structure) {
        Player owner = structure.getOwner();
        return owner == null || teamManager.isEnemy(actor, owner);
    }

    /** Destroys a structure with standard feedback (explosion particle + sound) at the given location. */
    public void demolish(PlacedStructure structure, Location feedbackLocation) {
        structureManager.remove(structure);
        feedbackLocation.getWorld().spawnParticle(Particle.EXPLOSION, feedbackLocation, 2);
        feedbackLocation.getWorld().playSound(feedbackLocation, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
    }
}