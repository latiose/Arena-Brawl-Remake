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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HoldToActivateTask extends BukkitRunnable {

    /** How many consecutive ticks the hand must be raised before it counts as a sustained hold. */
    private static final int HOLD_THRESHOLD_TICKS = 4;
    /** Minimum ticks between auto-repeat casts while holding. */
    private static final int HOLD_INTERVAL_TICKS = 2;
    /** Minimum ticks between casts triggered by individual clicks. */
    private static final int CLICK_INTERVAL_TICKS = 0;

    private final AbilityManager abilityManager;
    private final MatchManager matchManager;
    private final DebuffManager debuffManager;

    private final Set<UUID> pendingClicks = new HashSet<>();
    private final Map<UUID, Integer> holdTicks = new HashMap<>();
    private final Map<UUID, Long> lastActivation = new HashMap<>();
    private long currentTick = 0;

    public HoldToActivateTask(AbilityManager abilityManager, MatchManager matchManager, DebuffManager debuffManager) {
        this.abilityManager = abilityManager;
        this.matchManager = matchManager;
        this.debuffManager = debuffManager;
    }

    public void onRightClick(Player player) {
        pendingClicks.add(player.getUniqueId());
    }

    public void clear(UUID uuid) {
        pendingClicks.remove(uuid);
        holdTicks.remove(uuid);
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

            int heldFor;
            if (player.isHandRaised()) {
                heldFor = holdTicks.merge(uuid, 1, Integer::sum);
            } else {
                holdTicks.remove(uuid);
                heldFor = 0;
            }

            boolean isHold = heldFor >= HOLD_THRESHOLD_TICKS;
            boolean queued = pendingClicks.contains(uuid);
            if (!isHold && !queued) {
                continue;
            }

            if (debuffManager.hasDebuff(player, DebuffType.POLYMORPH)
                    || debuffManager.hasDebuff(player, DebuffType.STUN)
                    || debuffManager.hasDebuff(player, DebuffType.SILENCE)) {
                pendingClicks.remove(uuid);
                continue;
            }

            long interval = isHold ? HOLD_INTERVAL_TICKS : CLICK_INTERVAL_TICKS;
            if (currentTick - lastActivation.getOrDefault(uuid, Long.MIN_VALUE / 2) < interval) {
                continue;
            }

            Ability ability = abilityManager.getAbility(player, AbilitySlot.OFFENSIVE);
            if (ability == null) {
                pendingClicks.remove(uuid);
                continue;
            }

            boolean success = abilityManager.tryActivate(player, AbilitySlot.OFFENSIVE);
            if (success || isHold) {
                lastActivation.put(uuid, currentTick);
            }
            pendingClicks.remove(uuid);
        }
    }
}
