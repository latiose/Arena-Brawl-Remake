// powerups/PowerupTask.java
package org.latios.arenaBrawl.powerups;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;

import java.util.Map;

public class PowerupTask extends BukkitRunnable {

    private final MatchManager matchManager;
    private final PlayerHealthManager healthManager;
    private final DamageBuffManager damageBuffManager;

    public PowerupTask(MatchManager matchManager, PlayerHealthManager healthManager, DamageBuffManager damageBuffManager) {
        this.matchManager = matchManager;
        this.healthManager = healthManager;
        this.damageBuffManager = damageBuffManager;
    }

    @Override
    public void run() {
        Match match = matchManager.getActiveMatch();
        if (match == null) return;

        var players = match.getAllPlayers();
        if (players.isEmpty()) return;


        for (Player player : players) {
            if (player.isOnline()) {
                damageBuffManager.getMultiplier(player);
            }
        }

        long elapsed = System.currentTimeMillis() - match.getStartedAt();
        PowerupManager powerupManager = match.getPowerupManager();

        powerupManager.tick(elapsed, players);

        Map<PowerupType, Player> pickedUp = powerupManager.checkPickups(players);
        for (Map.Entry<PowerupType, Player> entry : pickedUp.entrySet()) {
            Player player = entry.getValue();

            switch (entry.getKey()) {
                case HEALTH -> {
                    healthManager.heal(player, 200);
                    player.sendMessage("§a§lYou picked up the Healing Powerup!");
                }
                case DAMAGE -> {
                    damageBuffManager.applyBuff(player, 2, 12_000);
                    player.sendMessage("§c§lYou picked up the Double Damage Powerup!");
                }
            }
        }
    }
}