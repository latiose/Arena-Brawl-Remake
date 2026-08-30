
package org.latios.arenaBrawl.debuffs;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SilenceListener implements DebuffListener {

    @Override
    public void onApplied(Player player, DebuffType type) {
        if (type != DebuffType.SILENCE) return;
    }

    @Override
    public void onExpired(Player player, DebuffType type) {

    }
}