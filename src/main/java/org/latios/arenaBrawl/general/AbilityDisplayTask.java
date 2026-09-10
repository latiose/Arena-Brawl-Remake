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
            String readyColorCode = getReadyColorCode(slot);
            Material unavailableMat = (slot.ordinal() == 0) ? AbilityKit.getIcon(slot) : Material.GRAY_DYE;
            setItem(player, slot, unavailableMat, 1, readyColorCode + ability.getName());
            return;
        }

        if (!cost.canPay(player)) {
            int remaining = Math.max(1, cost.getRemainingSeconds(player));

            Material cdMaterial = (slot.ordinal() == 0)
                    ? AbilityKit.getIcon(slot)
                    : Material.GRAY_DYE;
            String readyColorCode = getReadyColorCode(slot);
            String name = (slot.ordinal() == 0)
                    ? readyColorCode + ability.getName()
                    : readyColorCode + ability.getName() + " §8(" + remaining + "s)";

            int amount = (slot.ordinal() == 0) ? 1 : Math.min(remaining, 64);

            setItem(player, slot, cdMaterial, amount, name);
        } else {
            String readyColorCode = getReadyColorCode(slot);
            String readyName = readyColorCode + ability.getName() + " §f- §b§lRIGHT CLICK";

            setItem(player, slot, AbilityKit.getIcon(slot), 1, readyName);
        }
    }


    private String getReadyColorCode(AbilitySlot slot) {
        return switch (slot) {
            case OFFENSIVE -> "§c";
            case UTILITY -> "§e";
            case SUPPORT -> "§a";
            case ULTIMATE -> "§6";
        };
    }

    private void setItem(Player player, AbilitySlot slot, Material material, int amount, String name) {
        ItemStack item = AbilityKit.createAbilityItem(material, name);
        item.setAmount(amount);
        player.getInventory().setItem(slot.ordinal(), item);
    }
}