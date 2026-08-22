
package org.latios.arenaBrawl.hats;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.latios.arenaBrawl.stats.StatsManager;

import static me.libraryaddict.disguise.utilities.DisguiseUtilities.random;

public class MagicChestListener implements Listener {

    private final MagicChestGUI gui;
    private final KeyManager keyManager;
    private final MagicChestManager chestManager;
    private final StatsManager statsManager;

    public MagicChestListener(MagicChestGUI gui, KeyManager keyManager, MagicChestManager chestManager,StatsManager statsManager) {
        this.gui = gui;
        this.keyManager = keyManager;
        this.chestManager = chestManager;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.ENDER_CHEST) return;

        event.setCancelled(true);
        gui.open(event.getPlayer());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MagicChestGUI.MagicChestHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        Material clicked = event.getCurrentItem().getType();

        if (clicked == Material.TRIPWIRE_HOOK) {
            boolean bought = keyManager.buyKey(player);
            if (bought) {
                player.sendMessage("§aYou bought a key! You now have " + keyManager.getKeys(player) + " keys.");
                gui.open(player); // refresh the menu with updated counts
            } else {
                player.sendMessage("§cYou don't have enough coins (need " + KeyManager.getKeyCost() + ").");
            }
        } else if (clicked == Material.ENDER_CHEST) {
            boolean spent = keyManager.spendKey(player);
            if (!spent) {
                player.sendMessage("§cYou don't have any keys. Buy one first!");
                return;
            }

            player.closeInventory();
            MagicChestManager.ChestResult result = chestManager.open(player);

            if (result instanceof MagicChestManager.CoinsResult coinsResult) {
                player.sendMessage("§6§lYou got " + coinsResult.amount() + " coins!");
            } else if (result instanceof MagicChestManager.HatResult hatResult) {
                if (hatResult.wasNew()) {
                    player.sendMessage(hatResult.hat().rarity().getColor() + "§lNEW HAT: " + hatResult.hat().displayName() + "!");
                    player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 150);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                } else {
                    int amount = 50 + random.nextInt(151);
                    player.sendMessage(hatResult.hat().rarity().getColor() + "You got a duplicate: " + hatResult.hat().displayName()
                            + " §7(already unlocked), got "+amount+" coins instead!");
                    var stats = statsManager.getStats(player);
                    stats.coins += amount;
                    statsManager.saveDirectly(player, stats);
                }
            }
        }
    }
}