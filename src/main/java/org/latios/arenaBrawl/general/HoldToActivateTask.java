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

    private final AbilityManager abilityManager;
    private final MatchManager matchManager;
    private final DebuffManager debuffManager;

    private final Map<UUID, Integer> holdTicks = new HashMap<>();
    private static final int HOLD_THRESHOLD_TICKS = 5;

    public HoldToActivateTask(AbilityManager abilityManager, MatchManager matchManager, DebuffManager debuffManager) {
        this.abilityManager = abilityManager;
        this.matchManager = matchManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (!matchManager.isInMatch(player)
                    || player.getInventory().getHeldItemSlot() != AbilitySlot.OFFENSIVE.ordinal()
                    || !player.isHandRaised()) {
                holdTicks.remove(uuid);
                continue;
            }

            int currentTicks = holdTicks.getOrDefault(uuid, 0) + 1;
            holdTicks.put(uuid, currentTicks);

            if (currentTicks < HOLD_THRESHOLD_TICKS) {
                continue;
            }

            if (debuffManager.hasDebuff(player, DebuffType.POLYMORPH)
                    || debuffManager.hasDebuff(player, DebuffType.STUN)
                    || debuffManager.hasDebuff(player, DebuffType.SILENCE)) {
                continue;
            }

            Ability ability = abilityManager.getAbility(player, AbilitySlot.OFFENSIVE);
            if (ability == null) continue;

            if (ability.getCost().canPay(player)) {
                boolean success = abilityManager.tryActivate(player, AbilitySlot.OFFENSIVE);

                if (success) {
                    holdTicks.put(uuid, 0);
                } else {
                    holdTicks.put(uuid, 0);
                }
            }
        }
    }
}