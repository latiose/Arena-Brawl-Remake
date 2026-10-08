
package org.latios.arenaBrawl.abilities;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;

public class OrbitShieldSoundTask extends BukkitRunnable {

    private final OrbitShieldManager orbitShieldManager;
    public OrbitShieldSoundTask(OrbitShieldManager orbitShieldManager) {
        this.orbitShieldManager = orbitShieldManager;
    }

    @Override
    public void run() {
        for (var playerId : orbitShieldManager.getActiveShieldPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;

            OrbitShieldType type = orbitShieldManager.getActiveType(player);
            if (type == null) continue;
            player.getWorld().playSound(player.getLocation(), type.getAmbientSound(), 8.0f, 1.0f );
        }
    }
}