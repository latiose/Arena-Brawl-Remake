package org.latios.arenaBrawl.general;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class StatusBarUtil {

    private static final int TOTAL_BARS = 20;
    private static final String BLOCK_SYMBOL = "■";

    public static void sendStatusBar(Player player, String label, double progress, String mainColorHex) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        int filledBars = (int) Math.round(progress * TOTAL_BARS);

        StringBuilder barBuilder = new StringBuilder();

        barBuilder.append(ChatColor.WHITE);
        for (int i = 0; i < filledBars; i++) {
            barBuilder.append(BLOCK_SYMBOL);
        }
        barBuilder.append(ChatColor.DARK_GRAY);
        for (int i = filledBars; i < TOTAL_BARS; i++) {
            barBuilder.append(BLOCK_SYMBOL);
        }
        ChatColor labelColor = ChatColor.of(mainColorHex);
        String actionBarMessage = barBuilder.toString() + " " + labelColor + label;

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarMessage));
    }
}