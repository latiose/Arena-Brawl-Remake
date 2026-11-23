package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.latios.arenaBrawl.ArenaBrawlPlugin;

public class MobSpawnListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL
                || reason == CreatureSpawnEvent.SpawnReason.DEFAULT
                || reason == CreatureSpawnEvent.SpawnReason.JOCKEY) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getEntity() instanceof Zombie zombie)) return;

        // Vanilla may roll a chicken jockey / baby zombie during finalizeSpawn,
        // which can happen after this event. Normalize now and again next tick
        // so ability-spawned zombies always stay adult and never mount chickens.
        normalizeZombie(zombie);
        Bukkit.getScheduler().runTask(ArenaBrawlPlugin.getInstance(), () -> {
            if (zombie.isValid() && !zombie.isDead()) {
                normalizeZombie(zombie);
            }
        });
    }

    private void normalizeZombie(Zombie zombie) {
        EntityCleanupUtils.removeChickenJockey(zombie);
        if (!EntityCleanupUtils.isAllowedBabyZombie(zombie)) {
            zombie.setAdult();
        }
    }
}
