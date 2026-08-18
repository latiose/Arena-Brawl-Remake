package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class AbilityTriggerListener implements Listener {

    private final AbilityManager abilityManager;

    public AbilityTriggerListener(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();


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

        event.setCancelled(true);
        abilityManager.tryActivate(player, abilitySlot);
    }
}