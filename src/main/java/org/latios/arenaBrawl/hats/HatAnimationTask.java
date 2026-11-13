package org.latios.arenaBrawl.hats;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Cycles the material of animated hats (for example the Disco Hat) while they
 * are equipped. Schedule it with the desired interval (5 ticks = 0.25 seconds).
 */
public class HatAnimationTask extends BukkitRunnable {

    private final HatSelectionManager hatSelectionManager;

    public HatAnimationTask(HatSelectionManager hatSelectionManager) {
        this.hatSelectionManager = hatSelectionManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            HatDefinition hat = hatSelectionManager.getEquipped(player);
            if (hat == null) continue;

            List<Material> materials = hat.animatedMaterials();
            if (materials.size() < 2) continue;

            ItemStack helmet = player.getInventory().getHelmet();
            Material current = helmet != null ? helmet.getType() : null;

            int index = current != null ? materials.indexOf(current) : -1;
            Material next = materials.get((index + 1) % materials.size());

            if (current == next) continue;
            HatEquipUtils.applyEquippedHat(player, hatSelectionManager, next);
        }
    }
}
