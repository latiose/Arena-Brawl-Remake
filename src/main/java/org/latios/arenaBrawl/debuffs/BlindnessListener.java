package org.latios.arenaBrawl.debuffs;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BlindnessListener implements DebuffListener {

    @Override
    public void onApplied(Player player, DebuffType type) {
        if (type != DebuffType.BLINDNESS) return;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                PotionEffect.INFINITE_DURATION,
                0,
                true,
                false
        ));
    }

    @Override
    public void onExpired(Player player, DebuffType type) {
        if (type != DebuffType.BLINDNESS) return;
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }
}
