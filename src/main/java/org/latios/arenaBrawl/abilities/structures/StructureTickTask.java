
package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.scheduler.BukkitRunnable;

public class StructureTickTask extends BukkitRunnable {

    private final StructureManager structureManager;

    public StructureTickTask(StructureManager structureManager) {
        this.structureManager = structureManager;
    }

    @Override
    public void run() {
        structureManager.tickAll();
    }
}