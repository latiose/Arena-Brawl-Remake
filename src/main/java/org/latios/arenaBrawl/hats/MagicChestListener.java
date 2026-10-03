
package org.latios.arenaBrawl.hats;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.latios.arenaBrawl.general.ScoreboardManager;
import org.latios.arenaBrawl.lobby.LobbyScoreboardManager;
import org.latios.arenaBrawl.stats.StatsManager;

import java.util.concurrent.ThreadLocalRandom;

import static me.libraryaddict.disguise.utilities.DisguiseUtilities.random;

public class MagicChestListener implements Listener {

    private final MagicChestGUI gui;
    private final KeyManager keyManager;
    private final MagicChestManager chestManager;
    private final StatsManager statsManager;
    private final LobbyScoreboardManager scoreboardManager;
    private final ClickModeManager clickModeManager;
    public MagicChestListener(MagicChestGUI gui, KeyManager keyManager, MagicChestManager chestManager,
                              StatsManager statsManager, LobbyScoreboardManager scoreboardManager,ClickModeManager clickModeManager) {
        this.gui = gui;
        this.keyManager = keyManager;
        this.chestManager = chestManager;
        this.statsManager = statsManager;
        this.scoreboardManager = scoreboardManager;
        this.clickModeManager = clickModeManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
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
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int slot = event.getRawSlot();
        if (slot != MagicChestGUI.SLOT_BUY && slot != MagicChestGUI.SLOT_OPEN
                && slot != MagicChestGUI.SLOT_MODE) return;

        ClickType click = event.getClick();
        boolean realClick = click == ClickType.LEFT || click == ClickType.RIGHT;

        if (slot == MagicChestGUI.SLOT_MODE) {
            if (!realClick || !clickModeManager.passesCooldown(player)) return;
            clickModeManager.toggle(player);
            gui.refresh(event.getInventory(), player);
            return;
        }

        if (clickModeManager.isPrecise(player)) {
            if (!realClick) return;
            if (!clickModeManager.passesCooldown(player)) return;
        }


        if (slot == MagicChestGUI.SLOT_BUY) {
            boolean bought = keyManager.buyKey(player);
            if (bought) {
                player.sendMessage("§aYou bought a key! You now have " + keyManager.getKeys(player) + " keys.");
                gui.refresh(event.getInventory(), player);
                scoreboardManager.update(player);
            } else {
                player.sendMessage("§cYou don't have enough coins (need " + KeyManager.getKeyCost() + ").");
            }
        } else if (slot == MagicChestGUI.SLOT_OPEN) {
            boolean spent = keyManager.spendKey(player);
            if (!spent) {
                player.sendMessage("§cYou don't have any keys. Buy one first!");
                return;
            }

            MagicChestManager.ChestResult result = chestManager.open(player);

            if (result instanceof MagicChestManager.CoinsResult coinsResult) {
                player.sendMessage("§6§lYou got " + coinsResult.amount() + " coins!");
            } else if (result instanceof MagicChestManager.HatResult hatResult) {
                if (hatResult.wasNew()) {
                    String hatName = hatResult.hat().rarity().getColor() + hatResult.hat().displayName();
                    player.sendMessage(hatResult.hat().rarity().getColor() + "§lNEW HAT: " + hatResult.hat().displayName() + "!");
                    player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 150);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

                    String broadcastMessage = "§e" + player.getName() + " §7has unlocked the " + hatName + " §7hat!";
                    for (Player worldPlayer : player.getWorld().getPlayers()) {
                        if (!worldPlayer.equals(player)) {
                            worldPlayer.sendMessage(broadcastMessage);
                        }
                    }
                } else {
                    int amount = 50 + ThreadLocalRandom.current().nextInt(151);
                    player.sendMessage(hatResult.hat().rarity().getColor() + "You got a duplicate: " + hatResult.hat().displayName()
                            + " §7(already unlocked), got " + amount + " coins instead!");
                    var stats = statsManager.getStats(player);
                    stats.coins += amount;
                    statsManager.saveDirectly(player, stats);
                }
            }

            gui.refresh(event.getInventory(), player);
            scoreboardManager.update(player);
        }


    }


}