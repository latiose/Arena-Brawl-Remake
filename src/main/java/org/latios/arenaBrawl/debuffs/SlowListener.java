package org.latios.arenaBrawl.debuffs;


import org.bukkit.entity.Player;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SlowListener implements DebuffListener {
    private static final int BASE_AMPLIFIER = 1; //slow 2

    private final DebuffManager debuffManager;

    public SlowListener(DebuffManager debuffManager) {
        this.debuffManager = debuffManager;
    }

    @Override
    public void onApplied(Player player, DebuffType type) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                PotionEffect.INFINITE_DURATION,
                BASE_AMPLIFIER,
                true,
                false
        ));
    }

    @Override
    public void onExpired(Player player, DebuffType type) {
        debuffManager.clear(player);
    }
}
