package org.latios.arenaBrawl.general;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BaseSpeedTask extends BukkitRunnable {

    private static final int BASE_AMPLIFIER = 0; // Speed I

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyBaseSpeed(player);
        }
    }

    private void applyBaseSpeed(Player player) {


        PotionEffect currentSpeed = player.getPotionEffect(PotionEffectType.SPEED);

        if (currentSpeed != null)
            return;



        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                PotionEffect.INFINITE_DURATION,
                BASE_AMPLIFIER,
                true,
                false
        ));
    }
}