
package org.latios.arenaBrawl.abilities;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;

public class OrbitShieldSoundTask extends BukkitRunnable {

    private final OrbitShieldManager orbitShieldManager;
    private final MatchManager matchManager;
    public OrbitShieldSoundTask(OrbitShieldManager orbitShieldManager, MatchManager matchManager) {
        this.orbitShieldManager = orbitShieldManager;
        this.matchManager = matchManager;
    }

    @Override
    public void run() {
        for (var playerId : orbitShieldManager.getActiveShieldPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;

            OrbitShieldType type = orbitShieldManager.getActiveType(player);
            if (type == null) continue;
            MatchSoundUtils.play(matchManager, player, type.getAmbientSound(), 0.8f, 1f);
        }
    }
}