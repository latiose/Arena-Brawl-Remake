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
    public void onApplied(Player victim, DebuffType type) {
        if (type != DebuffType.ANTIHEAL) return;

        healthManager.disableRegen(victim, 5_000L);
    }

    @Override
    public void onExpired(Player victim, DebuffType type) {
        if (type != DebuffType.ANTIHEAL) return;

        healthManager.clearRegenDisable(victim);

        if (victim.isOnline() && !victim.isDead()) {
            victim.sendMessage(MessageUtils.positive() + "§3Corruption has worn off!");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        onExpired(event.getEntity(), DebuffType.ANTIHEAL);
    }
}