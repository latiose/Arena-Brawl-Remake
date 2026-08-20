package org.latios.arenaBrawl.lobby;



import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LobbyKit {

    public static void giveLobbyKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, buildCompass());
        player.getInventory().setItem(1, buildEmerald());
    }

    private static ItemStack buildCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bJoin Queue");
        meta.setLore(java.util.List.of("§7Right-click to join or leave the queue"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildEmerald() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aAbility Selector");
        meta.setLore(java.util.List.of("§7Right-click to choose your abilities"));
        item.setItemMeta(meta);
        return item;
    }
}