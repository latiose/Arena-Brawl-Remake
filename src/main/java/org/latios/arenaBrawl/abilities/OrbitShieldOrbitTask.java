
package org.latios.arenaBrawl.abilities;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class OrbitShieldOrbitTask extends BukkitRunnable {

    private final OrbitShieldManager orbitShieldManager;

    public OrbitShieldOrbitTask(OrbitShieldManager orbitShieldManager) {
        this.orbitShieldManager = orbitShieldManager;
    }

    @Override
    public void run() {
        orbitShieldManager.tickOrbits(Bukkit.getServer());
    }
}