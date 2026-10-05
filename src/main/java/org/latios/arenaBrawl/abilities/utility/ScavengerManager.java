package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.ArenaBrawlPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScavengerManager {

    private static final long PARTICLE_INTERVAL_TICKS = 20L;

    private final Map<UUID, Long> activeUntil = new HashMap<>();
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<>();

    public void activate(Player player, long durationMillis) {
        UUID id = player.getUniqueId();
        activeUntil.put(id, System.currentTimeMillis() + durationMillis);

        cancelParticleTask(id);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isActive(player)) {
                    cancel();
                    particleTasks.remove(id);
                    return;
                }
                if (player.getGameMode() == GameMode.SPECTATOR) return;

                player.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        player.getLocation().add(0, 1, 0),
                        12, 0.4, 0.6, 0.4, 0.05
                );
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), PARTICLE_INTERVAL_TICKS, PARTICLE_INTERVAL_TICKS);

        particleTasks.put(id, task);
    }

    public boolean isActive(Player player) {
        Long expiresAt = activeUntil.get(player.getUniqueId());
        if (expiresAt == null) return false;

        if (System.currentTimeMillis() > expiresAt) {
            activeUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void clear(Player player) {
        UUID id = player.getUniqueId();
        activeUntil.remove(id);
        cancelParticleTask(id);
    }

    private void cancelParticleTask(UUID id) {
        BukkitTask existing = particleTasks.remove(id);
        if (existing != null) existing.cancel();
    }
}