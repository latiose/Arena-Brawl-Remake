
package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.game.Match;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StructureManager {

    private final List<PlacedStructure> structures = new ArrayList<>();

    public void register(PlacedStructure structure) {
        structures.add(structure);
    }

    public void remove(PlacedStructure structure) {
        structure.remove();
        structures.remove(structure);
    }

    /** Call periodically to tick every structure and clean up expired ones. */
    public void tickAll() {
        var iterator = structures.iterator();
        while (iterator.hasNext()) {
            PlacedStructure structure = iterator.next();
            if (structure.tick()) {
                structure.remove();
                iterator.remove();
            }
        }
    }

    public List<PlacedStructure> getAll() {
        return structures;
    }


    public void clearForMatch(Match match) {
        if (match == null) return;

        Set<UUID> owners = new HashSet<>();
        for (Player player : match.getAllPlayers()) {
            owners.add(player.getUniqueId());
        }

        World matchWorld = match.getArenaMap() != null ? match.getArenaMap().getWorld() : null;

        List<PlacedStructure> toRemove = new ArrayList<>();
        for (PlacedStructure structure : structures) {
            if (structure.getOwnerId() == null || !owners.contains(structure.getOwnerId())) {
                continue;
            }

            World structureWorld = structure.getLocation() != null
                    ? structure.getLocation().getWorld()
                    : null;
            if (matchWorld != null && structureWorld != null && !matchWorld.equals(structureWorld)) {
                continue;
            }

            toRemove.add(structure);
        }

        for (PlacedStructure structure : toRemove) {
            structure.remove();
        }
        structures.removeAll(toRemove);
    }


    public void unregister(PlacedStructure structure) {
        structures.remove(structure);
    }

    public void clearAll() {
        List<PlacedStructure> toRemove = new ArrayList<>(structures);
        structures.clear();
        for (PlacedStructure structure : toRemove) {
            structure.remove();
        }
    }
}