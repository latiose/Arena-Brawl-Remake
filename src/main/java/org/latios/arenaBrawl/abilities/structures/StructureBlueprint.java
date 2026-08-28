package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Material;

import java.util.List;

public class StructureBlueprint {

    public record BlockOffset(int dx, int dy, int dz, Material material) {}

    private final List<BlockOffset> offsets;

    public StructureBlueprint(List<BlockOffset> offsets) {
        this.offsets = offsets;
    }

    public List<BlockOffset> getOffsets() {
        return offsets;
    }

}