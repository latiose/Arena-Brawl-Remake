
package org.latios.arenaBrawl.lobby;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.latios.arenaBrawl.hats.KeyManager;
import org.latios.arenaBrawl.lobby.ArenaChallengerGUI.RewardType;
import org.latios.arenaBrawl.lobby.ArenaChallengerGUI.TradeOption;
import org.latios.arenaBrawl.stats.PlayerStats;
import org.latios.arenaBrawl.stats.StatsManager;

public class ArenaChallengerListener implements Listener {

    private final ArenaChallengerGUI gui;
    private final KeyManager keyManager;
    private final StatsManager statsManager;

    public ArenaChallengerListener(ArenaChallengerGUI gui, KeyManager keyManager, StatsManager statsManager) {
        this.gui = gui;
        this.keyManager = keyManager;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ArenaChallengerGUI.ChallengerHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        TradeOption trade = ArenaChallengerGUI.getTrade(event.getRawSlot());
        if (trade == null) return;

        PlayerStats stats = statsManager.getStats(player);
        if (stats.winStreak < trade.requiredStreak()) {
            player.sendMessage("§cYou need a win streak of §e" + trade.requiredStreak()
                    + "§c to claim this!");
            return;
        }

        claimReward(player, trade);
        statsManager.resetWinStreak(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        gui.refresh(event.getInventory(), player);
    }

    private void claimReward(Player player, TradeOption trade) {
        if (trade.reward() == RewardType.KEYS) {
            keyManager.addKeys(player, trade.rewardAmount());
            player.sendMessage("§b§lYou claimed " + trade.rewardAmount() + " keys!");
        } else {
            PlayerStats stats = statsManager.getStats(player);
            stats.coins += trade.rewardAmount();
            statsManager.saveDirectly(player, stats);
            player.sendMessage("§6§lYou claimed " + trade.rewardAmount() + " coins!");
        }
    }
}
