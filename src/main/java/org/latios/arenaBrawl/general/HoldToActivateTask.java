package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.game.MatchManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoldToActivateTask extends BukkitRunnable {
    private static final int HOLD_GRACE_TICKS = 1;
    private static final int ACTIVATION_INTERVAL_TICKS = 2;

    private final AbilityManager abilityManager;
    private final MatchManager matchManager;
    private final DebuffManager debuffManager;

    private final Map<UUID, Long> lastRightClick = new HashMap<>();
    private final Map<UUID, Long> lastActivation = new HashMap<>();
    private long currentTick = 0;

    public HoldToActivateTask(AbilityManager abilityManager, MatchManager matchManager, DebuffManager debuffManager) {
        this.abilityManager = abilityManager;
        this.matchManager = matchManager;
        this.debuffManager = debuffManager;
    }

    public void markRightClick(Player player) {
        lastRightClick.put(player.getUniqueId(), currentTick);
    }

    public void clear(UUID uuid) {
        lastRightClick.remove(uuid);
        lastActivation.remove(uuid);
    }

    @Override
    public void run() {
        currentTick++;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (!matchManager.isInMatch(player)
                    || player.getInventory().getHeldItemSlot() != AbilitySlot.OFFENSIVE.ordinal()) {
                clear(uuid);
                continue;
            }

            long lastClick = lastRightClick.getOrDefault(uuid, Long.MIN_VALUE / 2);
            boolean holding = player.isHandRaised() || (currentTick - lastClick) <= HOLD_GRACE_TICKS;
            if (!holding) {
                continue;
            }

            if (currentTick - lastActivation.getOrDefault(uuid, Long.MIN_VALUE / 2) < ACTIVATION_INTERVAL_TICKS) {
                continue;
            }

            if (debuffManager.hasDebuff(player, DebuffType.POLYMORPH)
                    || debuffManager.hasDebuff(player, DebuffType.STUN)
                    || debuffManager.hasDebuff(player, DebuffType.SILENCE)) {
                continue;
            }

            Ability ability = abilityManager.getAbility(player, AbilitySlot.OFFENSIVE);
            if (ability == null) {
                continue;
            }

            if (abilityManager.tryActivate(player, AbilitySlot.OFFENSIVE)) {
                lastActivation.put(uuid, currentTick);
            }
        }
    }
}