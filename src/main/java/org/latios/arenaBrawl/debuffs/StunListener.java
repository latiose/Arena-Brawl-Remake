
package org.latios.arenaBrawl.debuffs;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class StunListener implements DebuffListener {

    private final DebuffManager debuffManager;

    public StunListener(DebuffManager debuffManager) {
        this.debuffManager = debuffManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!debuffManager.hasDebuff(player, DebuffType.STUN)) return;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                PotionEffect.INFINITE_DURATION,
                1,
                true,
                false
        ));
        // Cancel horizontal movement but allow looking around (only block if position actually changed)
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getZ() != event.getTo().getZ()
                || event.getFrom().getY() != event.getTo().getY()) {
            event.setTo(event.getFrom());
        }
    }
}