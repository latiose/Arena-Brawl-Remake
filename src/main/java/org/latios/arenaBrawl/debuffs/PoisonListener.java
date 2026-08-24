// debuffs/PoisonListener.java
package org.latios.arenaBrawl.debuffs;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PoisonListener implements DebuffListener {

    private static final double DAMAGE_PER_SECOND = 33.0;
    private static final long DURATION_TICKS = 120; // 6 seconds, matches DURATION_MILLIS in BroodMother

    private final PlayerHealthManager healthManager;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    public PoisonListener(PlayerHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public void onApplied(Player player, DebuffType type) {
        if (type != DebuffType.POISON) return;

        // Vanilla poison effect for the green tint/particles/icon, purely visual —
        // the actual damage is handled manually below so it can bypass shields.
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.POISON, (int) DURATION_TICKS, 0, true, true
        ));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    activeTasks.remove(player.getUniqueId());
                    return;
                }
                healthManager.damage(player, DAMAGE_PER_SECOND);

                // "[Attacker]'s [Ability] hit you for [Damage] damage."
                player.sendMessage(MessageUtils.negative()+String.format(
                        "§3Broodmothers' poison hit §3you §3for §c%d §3damage.",DAMAGE_PER_SECOND)
                );
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 20L, 20L);

        activeTasks.put(player.getUniqueId(), task);
    }

    @Override
    public void onExpired(Player player, DebuffType type) {
        if (type != DebuffType.POISON) return;

        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        player.removePotionEffect(PotionEffectType.POISON);
    }
}