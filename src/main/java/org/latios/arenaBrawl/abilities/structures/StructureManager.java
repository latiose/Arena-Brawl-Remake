
package org.latios.arenaBrawl.abilities.structures;

import java.util.ArrayList;
import java.util.List;

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

    public void clearAll() {
        for (PlacedStructure structure : structures) {
            structure.remove();
        }
        structures.clear();
    }
}