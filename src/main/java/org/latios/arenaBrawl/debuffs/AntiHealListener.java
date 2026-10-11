package org.latios.arenaBrawl.debuffs;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;

public class AntiHealListener implements DebuffListener, Listener {

    private final PlayerHealthManager healthManager;

    public AntiHealListener(PlayerHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public void onApplied(Player victim, DebuffType type, long durationMillis, double strength) {
        if (type != DebuffType.ANTIHEAL) return;

        healthManager.addHealReduction(victim, strength, durationMillis);
    }

    @Override
    public void onExpired(Player victim, DebuffType type) {
        if (type != DebuffType.ANTIHEAL) return;
    }

    @Override
    public void onExpired(Player victim, DebuffType type, long durationMillis, double strength) {
        if (type != DebuffType.ANTIHEAL) return;

        healthManager.removeHealReduction(victim, strength, durationMillis);
        if (victim.isOnline() && !victim.isDead()) {
            victim.sendMessage(MessageUtils.positive() + "§3Anti healing has worn off!");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        healthManager.clearRegenDisable(event.getEntity());
    }
}