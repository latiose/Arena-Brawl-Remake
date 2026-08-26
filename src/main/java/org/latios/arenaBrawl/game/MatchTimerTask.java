package org.latios.arenaBrawl.game;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;

public class MatchTimerTask extends BukkitRunnable {

    private static final long DOUBLE_DAMAGE_AT_MILLIS = 5 * 60_000L;
    private static final long DRAW_AT_MILLIS = 10* 60_000L;

    private final MatchManager matchManager;

    public MatchTimerTask(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @Override
    public void run() {
        List<Match> matches = new ArrayList<>(matchManager.getActiveMatches());

        for (Match match : matches) {
            if (match.isEnded()) continue;

            long elapsed = System.currentTimeMillis() - match.getStartedAt();

            if (!match.isDoubleDamageActive() && elapsed >= DOUBLE_DAMAGE_AT_MILLIS) {
                match.setDoubleDamageActive(true);
                for (Player player : match.getAllPlayers()) {
                    if (player.isOnline()) player.sendMessage("§c§lDOUBLE DAMAGE IS NOW ACTIVE FOR EVERYONE!");
                }
            }

            if (elapsed >= DRAW_AT_MILLIS) {
                matchManager.endMatchAsDraw(match);
            }
        }
    }
}