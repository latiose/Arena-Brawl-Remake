// debuffs/PolymorphNameUpdateTask.java
package org.latios.arenaBrawl.debuffs;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.general.PlayerHealthManager;

public class PolymorphNameUpdateTask extends BukkitRunnable {

    private final DebuffManager debuffManager;
    private final PlayerHealthManager healthManager;

    public PolymorphNameUpdateTask(DebuffManager debuffManager, PlayerHealthManager healthManager) {
        this.debuffManager = debuffManager;
        this.healthManager = healthManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!debuffManager.hasDebuff(player, DebuffType.POLYMORPH)) continue;
            if (!DisguiseAPI.isDisguised(player)) continue;

            Disguise disguise = DisguiseAPI.getDisguise(player);
            if (disguise.getWatcher() instanceof LivingWatcher watcher) {
                int hp = (int) healthManager.getHealth(player);
                watcher.setCustomName("§f" + player.getName() + " §7- §c" + hp + " HP");
            }
        }
    }
}