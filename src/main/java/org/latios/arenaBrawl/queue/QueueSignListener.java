package org.latios.arenaBrawl.queue;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.game.MatchType;

public class QueueSignListener implements Listener {
    private final QueueManager queueManager;
    private final MatchManager matchManager;

    public QueueSignListener(QueueManager queueManager, MatchManager matchManager) {
        this.queueManager = queueManager;
        this.matchManager = matchManager;
    }


    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || !(event.getClickedBlock().getState() instanceof Sign sign)) return;

        MatchType type = getMatchType(sign);
        if (type == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (matchManager.isInMatch(player)) {
            player.sendMessage("§cYou can't use the queue while in a match.");
            return;
        }

        if (queueManager.isQueued(player, type)) {
            queueManager.leaveQueue(player, type);
            player.sendMessage("§eYou left the " + type.getDisplayName() + " queue.");
            return;
        }

        int size = queueManager.joinQueue(player, type);
        if (size == -1) {
            player.sendMessage("§cA member of your party is already in a queue or the party is too large.");
        } else {
            player.sendMessage("§aYou joined " + type.getDisplayName()
                    + " (" + size + "/" + type.playersNeeded() + ").");
        }
    }

    private MatchType getMatchType(Sign sign) {
        String text = String.join(" ", sign.getSide(org.bukkit.block.sign.Side.FRONT).getLines());
        if (text.toLowerCase().contains("ffa")) return MatchType.FFA;
        if (text.toLowerCase().contains("2v2")) return MatchType.TEAMS;
        if (text.toLowerCase().contains("1v1")) return MatchType.DUEL;
        return null;
    }
}
