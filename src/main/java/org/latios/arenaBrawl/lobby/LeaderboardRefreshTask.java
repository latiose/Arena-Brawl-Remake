package org.latios.arenaBrawl.lobby;

import org.bukkit.scheduler.BukkitRunnable;

public class LeaderboardRefreshTask extends BukkitRunnable {

    private final LeaderboardSignManager signManager;

    public LeaderboardRefreshTask(LeaderboardSignManager signManager) {
        this.signManager = signManager;
    }

    @Override
    public void run() {
        signManager.refresh();
    }
}