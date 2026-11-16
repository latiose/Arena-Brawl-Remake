package org.latios.arenaBrawl.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.latios.arenaBrawl.gui.PaginationUtil;

public class CosmeticEffectListener implements Listener {

    private final CosmeticEffectManager effectManager;
    private final HitEffectSelectorGUI hitEffectGUI;
    private final KillEffectSelectorGUI killEffectGUI;

    public CosmeticEffectListener(CosmeticEffectManager effectManager,
                                  HitEffectSelectorGUI hitEffectGUI,
                                  KillEffectSelectorGUI killEffectGUI) {
        this.effectManager = effectManager;
        this.hitEffectGUI = hitEffectGUI;
        this.killEffectGUI = killEffectGUI;
    }

    @EventHandler
    public void onHitEffectClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HitEffectSelectorGUI.HitEffectHolder holder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        HitEffectType[] effects = HitEffectType.values();
        int totalPages = PaginationUtil.totalPages(effects.length);
        int rawSlot = event.getRawSlot();

        if (PaginationUtil.isPrevious(rawSlot, holder.page())) {
            hitEffectGUI.open(player, holder.page() - 1);
            return;
        }
        if (PaginationUtil.isNext(rawSlot, holder.page(), totalPages)) {
            hitEffectGUI.open(player, holder.page() + 1);
            return;
        }

        if (rawSlot == HitEffectSelectorGUI.RANDOM_SLOT) {
            effectManager.equipRandomHitEffect(player);
            player.sendMessage("§aHit effect set to: §dRandom §7(rolled each match)");
            hitEffectGUI.open(player, holder.page());
            return;
        }

        int index = PaginationUtil.indexOf(rawSlot, holder.page());
        if (index < 0 || index >= effects.length) return;

        HitEffectType clicked = effects[index];

        if (!effectManager.isHitUnlocked(player, clicked)) {
            CosmeticEffectManager.PurchaseResult result = effectManager.buyHitEffect(player, clicked);
            switch (result) {
                case SUCCESS -> player.sendMessage("§aYou bought and equipped "
                        + clicked.getColor() + clicked.getDisplayName() + "§a!");
                case NOT_ENOUGH_COINS -> player.sendMessage("§cYou don't have enough coins (need "
                        + clicked.getPrice() + ").");
                case ALREADY_UNLOCKED -> { }
            }
        } else if (effectManager.getEquippedHitEffect(player) == clicked) {
            effectManager.unequipHitEffect(player);
            player.sendMessage("§eYou disabled your hit effect.");
        } else {
            effectManager.equipHitEffect(player, clicked);
            player.sendMessage("§aYou equipped " + clicked.getColor() + clicked.getDisplayName() + "§a!");
        }

        hitEffectGUI.open(player, holder.page());
    }

    @EventHandler
    public void onKillEffectClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KillEffectSelectorGUI.KillEffectHolder holder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        KillEffectType[] effects = KillEffectType.values();
        int totalPages = PaginationUtil.totalPages(effects.length);
        int rawSlot = event.getRawSlot();

        if (PaginationUtil.isPrevious(rawSlot, holder.page())) {
            killEffectGUI.open(player, holder.page() - 1);
            return;
        }
        if (PaginationUtil.isNext(rawSlot, holder.page(), totalPages)) {
            killEffectGUI.open(player, holder.page() + 1);
            return;
        }

        if (rawSlot == KillEffectSelectorGUI.RANDOM_SLOT) {
            effectManager.equipRandomKillEffect(player);
            player.sendMessage("§aKill effect set to: §dRandom §7(rolled each match)");
            killEffectGUI.open(player, holder.page());
            return;
        }

        int index = PaginationUtil.indexOf(rawSlot, holder.page());
        if (index < 0 || index >= effects.length) return;

        KillEffectType clicked = effects[index];

        if (!effectManager.isKillUnlocked(player, clicked)) {
            CosmeticEffectManager.PurchaseResult result = effectManager.buyKillEffect(player, clicked);
            switch (result) {
                case SUCCESS -> player.sendMessage("§aYou bought and equipped "
                        + clicked.getColor() + clicked.getDisplayName() + "§a!");
                case NOT_ENOUGH_COINS -> player.sendMessage("§cYou don't have enough coins (need "
                        + clicked.getPrice() + ").");
                case ALREADY_UNLOCKED -> { }
            }
        } else if (effectManager.getEquippedKillEffect(player) == clicked) {
            effectManager.unequipKillEffect(player);
            player.sendMessage("§eYou disabled your kill effect.");
        } else {
            effectManager.equipKillEffect(player, clicked);
            player.sendMessage("§aYou equipped " + clicked.getColor() + clicked.getDisplayName() + "§a!");
        }

        killEffectGUI.open(player, holder.page());
    }
}
