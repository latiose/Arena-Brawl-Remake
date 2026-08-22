// debuffs/PolymorphHealTask.java
package org.latios.arenaBrawl.debuffs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.general.PlayerHealthManager;

public class PolymorphHealTask extends BukkitRunnable {

    private static final double HEAL_PER_SECOND = 25.0;

    private final DebuffManager debuffManager;
    private final PlayerHealthManager healthManager;

    public PolymorphHealTask(DebuffManager debuffManager, PlayerHealthManager healthManager) {
        this.debuffManager = debuffManager;
        this.healthManager = healthManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (debuffManager.hasDebuff(player, DebuffType.POLYMORPH)) {
                healthManager.heal(player, HEAL_PER_SECOND);
                player.sendMessage("§aPolymorph healed you for " + HEAL_PER_SECOND + "health!");
            }
        }
    }
}