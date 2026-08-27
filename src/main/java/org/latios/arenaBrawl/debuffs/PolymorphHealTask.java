package org.latios.arenaBrawl.debuffs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.general.MessageUtils;
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
        for (Player victim : Bukkit.getOnlinePlayers()) {
            if (debuffManager.hasDebuff(victim, DebuffType.POLYMORPH)) {
                healthManager.heal(victim, HEAL_PER_SECOND);
                int roundedHeal = (int) Math.round(HEAL_PER_SECOND);
                Player attacker = debuffManager.getAttacker(victim);
                victim.sendMessage(MessageUtils.positive() + "§c"+attacker.getName()+ "§3's Polymorph healed you for §a" + roundedHeal + "§3 health!");

                if (attacker.isOnline()) {
                    attacker.sendMessage(MessageUtils.positive() + "§3Your Polymorph healed §c" + victim.getName() + "§3 for §a" + roundedHeal + "§3 health!");
                }
            }
        }
    }
}