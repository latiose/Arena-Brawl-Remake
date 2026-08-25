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
            Player picker = entry.getValue();
            PowerupType type = entry.getKey();

            switch (type) {
                case HEALTH -> healthManager.heal(picker, 200);
                case DOUBLE_DAMAGE -> damageBuffManager.applyBuff(picker, 2, 12_000);
            }

            String powerupName = type == PowerupType.HEALTH ? "HEALING" : "DOUBLE DAMAGE";

            boolean isPickerRed = match.getRed().contains(picker);

            for (Player p : players) {
                if (!p.isOnline()) continue;

                if (p.equals(picker)) {

                    if (type == PowerupType.HEALTH) {
                        p.sendMessage("§aYou activated the Healing Powerup!");
                        p.sendMessage("§a+200 health!");
                    } else if (type == PowerupType.DOUBLE_DAMAGE) {
                        p.sendMessage("§cYou activated the Double Damage Powerup!");
                    }
                } else {
                    boolean isViewerRed = match.getRed().contains(p);
                    boolean isTeammate = (isPickerRed == isViewerRed);

                    String color = isTeammate ? "§a" : "§c";

                    p.sendMessage(color + picker.getName() + " §eactivated the §a" + powerupName + " §epowerup!");
                }
            }
        }
    }
}