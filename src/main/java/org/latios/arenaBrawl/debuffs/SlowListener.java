// debuffs/SlowListener.java
package org.latios.arenaBrawl.debuffs;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SlowListener implements DebuffListener {

    private static final int AMPLIFIER = 1; // Slowness II

    @Override
    public void onApplied(Player player, DebuffType type) {
        if (type != DebuffType.SLOW) return;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                PotionEffect.INFINITE_DURATION,
                AMPLIFIER,
                true,
                false
        ));
    }

    @Override
    public void onExpired(Player player, DebuffType type) {
        if (type != DebuffType.SLOW) return;
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }
}