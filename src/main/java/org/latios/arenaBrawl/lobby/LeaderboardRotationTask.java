package org.latios.arenaBrawl.lobby;

import org.bukkit.scheduler.BukkitRunnable;

public class LeaderboardRotationTask extends BukkitRunnable {

    private final LeaderboardSignManager signManager;

    public LeaderboardRotationTask(LeaderboardSignManager signManager) {
        this.signManager = signManager;
    }

    @Override
    public void run() {
        signManager.rotate();
    }
}