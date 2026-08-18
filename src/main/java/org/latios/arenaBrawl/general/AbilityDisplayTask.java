
package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityManager;
import org.latios.arenaBrawl.abilities.AbilitySlot;

public class AbilityDisplayTask extends BukkitRunnable {

    private final AbilityManager abilityManager;

    public AbilityDisplayTask(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (AbilitySlot slot : AbilitySlot.values()) {
                updateSlot(player, slot);
            }
        }
    }

    private void updateSlot(Player player, AbilitySlot slot) {
        Ability ability = abilityManager.getAbility(player, slot);
        if (ability == null) return;

        AbilityCost cost = ability.getCost();

        if (cost.isPermanentlyUnavailable(player)) {
            setItem(player, slot, Material.GRAY_DYE, 1, "§7" + ability.getName() + " §8(used)");
            return;
        }

        int remaining = cost.getRemainingSeconds(player);

        if (remaining > 0) {
            setItem(player, slot, Material.GRAY_DYE, Math.min(remaining, 64),
                    "§7" + ability.getName() + " §8(" + remaining + "s)");
        } else {
            setItem(player, slot, AbilityKit.getIcon(slot), 1, "§e" + ability.getName());
        }
    }

    private void setItem(Player player, AbilitySlot slot, Material material, int amount, String name) {
        ItemStack current = player.getInventory().getItem(slot.ordinal());

        if (current != null && current.getType() == material && current.getAmount() == amount
                && current.hasItemMeta() && name.equals(current.getItemMeta().getDisplayName())) {
            return;
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        player.getInventory().setItem(slot.ordinal(), item);
    }
}