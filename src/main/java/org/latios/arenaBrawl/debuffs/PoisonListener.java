package org.latios.arenaBrawl.debuffs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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

public class PoisonListener implements DebuffListener, Listener {

    private static final double DAMAGE_PER_SECOND = 33.0;
    private static final long DURATION_TICKS = 120; // 6

    private final PlayerHealthManager healthManager;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, UUID> activeAttackers = new HashMap<>();

    public PoisonListener(PlayerHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    public void onAppliedWithAttacker(Player victim, Player attacker, DebuffType type) {
        if (type != DebuffType.POISON) return;

        onExpired(victim, DebuffType.POISON);

        if (attacker != null) {
            activeAttackers.put(victim.getUniqueId(), attacker.getUniqueId());
        }

        victim.addPotionEffect(new PotionEffect(
                PotionEffectType.POISON, (int) DURATION_TICKS, 0, true, true
        ));

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!victim.isOnline() || victim.isDead()) {
                    onExpired(victim, DebuffType.POISON);
                    return;
                }

                int roundedDamage = (int) Math.round(DAMAGE_PER_SECOND);
                healthManager.damage(victim, DAMAGE_PER_SECOND);
                victim.playHurtAnimation(0);
                victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1f, 1f);

                UUID attackerId = activeAttackers.get(victim.getUniqueId());
                if (attackerId != null) {
                    Player attackerPlayer = Bukkit.getPlayer(attackerId);
                    assert attackerPlayer != null;
                    victim.sendMessage(MessageUtils.negative() + String.format(
                        "§3%s's poison hit §3you §3for §c%d §3damage.",attackerPlayer.getName(), roundedDamage
                ));

                    if (attackerPlayer.isOnline()) {
                        attackerPlayer.sendMessage(MessageUtils.positive() + String.format(
                                "§3Your poison hit §3%s §3for §c%d §3damage.", victim.getName(), roundedDamage
                        ));
                    }
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 20L, 20L);

        activeTasks.put(victim.getUniqueId(), task);
    }

    @Override
    public void onApplied(Player player, DebuffType type) {
        onAppliedWithAttacker(player, null, type);
    }

    @Override
    public void onExpired(Player player, DebuffType type) {
        if (type != DebuffType.POISON) return;

        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        activeAttackers.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.POISON);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        onExpired(event.getEntity(), DebuffType.POISON);
    }
}