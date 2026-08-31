package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.PlayerHealthManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EtherealBodyManager {

    private final PlayerHealthManager healthManager;
    private final Map<UUID, Double> activeShields = new HashMap<>();

    public EtherealBodyManager(PlayerHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    public void activate(Player player, long durationMillis) {
        UUID uuid = player.getUniqueId();
        activeShields.put(uuid, 0.0);

        long durationTicks = durationMillis / 50L;

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !isActive(player)) {
                    activeShields.remove(uuid);
                    cancel();
                    return;
                }

                if (ticksElapsed >= durationTicks) {
                    double accumulatedDamage = activeShields.remove(uuid);
                    if (accumulatedDamage > 0) {
                        healthManager.heal(player, accumulatedDamage, "Ethereal Body");

                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.5f);
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.0, 0), 30, 0.4, 0.6, 0.4, 0.2);
                    }
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                double angle = ticksElapsed * 0.4;
                double x = Math.cos(angle) * 0.8;
                double z = Math.sin(angle) * 0.8;
                double y = (ticksElapsed % 20) * 0.1;

                loc.getWorld().spawnParticle(
                        Particle.DUST,
                        loc.clone().add(x, y, z),
                        1,
                        new Particle.DustOptions(Color.fromRGB(150, 240, 255), 1.2f)
                );

                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.0, 0), 1, 0.3, 0.4, 0.3, 0.01);

                if (ticksElapsed % 10 == 0) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
                }

                ticksElapsed += 2;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 2L);
    }

    public boolean isActive(Player player) {
        return activeShields.containsKey(player.getUniqueId());
    }

    public boolean processIncomingDamage(Player victim, double damage) {
        if (!isActive(victim)) return false;

        UUID uuid = victim.getUniqueId();
        double currentStored = activeShields.getOrDefault(uuid, 0.0);
        activeShields.put(uuid, currentStored + damage);

        Location loc = victim.getLocation().add(0, 1.0, 0);
        victim.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 2.0f);
        victim.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 10, 0.3, 0.5, 0.3, 0.05);

        return true;
    }
}