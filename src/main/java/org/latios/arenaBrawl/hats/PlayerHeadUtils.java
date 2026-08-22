
package org.latios.arenaBrawl.hats;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class PlayerHeadUtils {

    public static ItemStack createPlayerHead(UUID playerUuid) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUuid)); // UUID lookup is local, no network call
        head.setItemMeta(meta);
        return head;
    }

    // Overload kept for convenience/testing, but prefer the UUID version in production code
    public static ItemStack createPlayerHead(String playerName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        head.setItemMeta(meta);
        return head;
    }
}