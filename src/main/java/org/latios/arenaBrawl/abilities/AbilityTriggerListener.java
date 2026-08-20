package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.latios.arenaBrawl.abilities.support.HolyWater; // Importar HolyWater
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.game.MatchManager;

public class AbilityTriggerListener implements Listener {

    private final AbilityManager abilityManager;
    private final MatchManager matchManager;
    private final DebuffManager debuffManager;

    public AbilityTriggerListener(AbilityManager abilityManager, DebuffManager debuffManager, MatchManager matchManager) {
        this.abilityManager = abilityManager;
        this.debuffManager = debuffManager;
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (!matchManager.isInMatch(player)) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        int heldSlot = player.getInventory().getHeldItemSlot();

        AbilitySlot abilitySlot = switch (heldSlot) {
            case 0 -> AbilitySlot.OFFENSIVE;
            case 1 -> AbilitySlot.UTILITY;
            case 2 -> AbilitySlot.SUPPORT;
            case 3 -> AbilitySlot.ULTIMATE;
            default -> null;
        };

        if (abilitySlot == null) {
            return;
        }

        Ability currentAbility = abilityManager.getAbility(player, abilitySlot);

        if (debuffManager.hasDebuff(player, DebuffType.POLYMORPH)
                || debuffManager.hasDebuff(player, DebuffType.STUN)) {
            if (!(currentAbility instanceof HolyWater)) {
                player.sendMessage("§cYou can't use abilities right now!");
                return;
            }
        }


        event.setCancelled(true);
        abilityManager.tryActivate(player, abilitySlot);
    }
}