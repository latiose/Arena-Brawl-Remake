
package org.latios.arenaBrawl.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import org.latios.arenaBrawl.hats.KeyManager;
import org.latios.arenaBrawl.stats.PlayerStats;
import org.latios.arenaBrawl.stats.StatsManager;

import java.util.List;

public class ArenaChallengerGUI {

    public static final int SLOT_STREAK_2 = 10;
    public static final int SLOT_STREAK_4 = 12;
    public static final int SLOT_STREAK_6 = 14;
    public static final int SLOT_STREAK_10 = 16;

    public record ChallengerHolder() implements InventoryHolder {
        @Override
        public @NonNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }

    public enum RewardType {
        COINS,
        KEYS
    }

    public record TradeOption(int requiredStreak, RewardType reward, int rewardAmount, Material icon) {
    }

    public static final List<TradeOption> TRADES = List.of(
            new TradeOption(2, RewardType.COINS, 1000, Material.GOLD_INGOT),
            new TradeOption(4, RewardType.COINS, 2500, Material.GOLD_BLOCK),
            new TradeOption(6, RewardType.KEYS, 5, Material.TRIPWIRE_HOOK),
            new TradeOption(10, RewardType.KEYS, 10, Material.TRIPWIRE_HOOK)
    );

    public static TradeOption getTrade(int slot) {
        return switch (slot) {
            case SLOT_STREAK_2 -> TRADES.get(0);
            case SLOT_STREAK_4 -> TRADES.get(1);
            case SLOT_STREAK_6 -> TRADES.get(2);
            case SLOT_STREAK_10 -> TRADES.get(3);
            default -> null;
        };
    }

    private final KeyManager keyManager;
    private final StatsManager statsManager;

    public ArenaChallengerGUI(KeyManager keyManager, StatsManager statsManager) {
        this.keyManager = keyManager;
        this.statsManager = statsManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new ChallengerHolder(), 27, "§e§lArena Challenger");
        refresh(inv, player);
        player.openInventory(inv);
    }

    public void refresh(Inventory inv, Player player) {
        PlayerStats stats = statsManager.getStats(player);

        int slot = SLOT_STREAK_2;
        for (TradeOption trade : TRADES) {
            inv.setItem(slot, buildTradeItem(trade, player));
            slot += 2;
        }
    }

    private ItemStack buildTradeItem(TradeOption trade, Player player) {
        PlayerStats stats = statsManager.getStats(player);
        ItemStack item = new ItemStack(trade.icon());
        ItemMeta meta = item.getItemMeta();

        String rewardText = trade.reward() == RewardType.KEYS
                ? "§b" + trade.rewardAmount() + " Keys"
                : "§6" + trade.rewardAmount() + " Coins";

        meta.displayName(Component.text(trade.requiredStreak() + " Win Streak", NamedTextColor.YELLOW, TextDecoration.BOLD));

        List<String> lore;
        if (stats.winStreak >= trade.requiredStreak()) {
            lore = List.of(
                    "§7Reward: " + rewardText,
                    "§7Your streak: §e" + stats.winStreak,
                    "§7Your coins: §6" + stats.coins,
                    "§7Your keys: §b" + keyManager.getKeys(player),
                    "",
                    "§aClick to claim!"
            );
        } else {
            lore = List.of(
                    "§7Reward: " + rewardText,
                    "§7Your streak: §e" + stats.winStreak,
                    "",
                    "§cWin §f" + (trade.requiredStreak() - stats.winStreak)
                            + " §cmore consecutive matches to claim."
            );
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
