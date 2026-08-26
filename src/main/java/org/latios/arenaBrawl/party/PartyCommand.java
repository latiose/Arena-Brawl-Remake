// party/PartyCommand.java
package org.latios.arenaBrawl.party;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.latios.arenaBrawl.queue.QueueManager;

public class PartyCommand implements CommandExecutor {

    private final PartyManager partyManager;
    private final QueueManager queueManager;

    public PartyCommand(PartyManager partyManager, QueueManager queueManager) {
        this.partyManager = partyManager;
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (queueManager.isQueued(player)) {
            player.sendMessage("§cLeave the queue before managing your party.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /party <invite|accept|leave> [player]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "leave" -> handleLeave(player);
            default -> player.sendMessage("§cUnknown subcommand.");
        }

        return true;
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§cUsage: /party invite <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        if (queueManager.isQueued(target)) {
            player.sendMessage("§c" + target.getName() + " is currently in queue and can't be invited.");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou can't invite yourself.");
            return;
        }

        Party party = partyManager.getParty(player);
        if (party != null && !party.isLeader(player)) {
            player.sendMessage("§cOnly the party leader can invite.");
            return;
        }

        if (party != null && partyManager.isFull(party)) {
            player.sendMessage("§cYour party is already full.");
            return;
        }

        if (party == null) {
            partyManager.createParty(player);
        }

        partyManager.invite(player, target);
        player.sendMessage("§aInvite sent to " + target.getName() + ".");
        target.sendMessage("§e" + player.getName() + " invited you to their party. §a/party accept " + player.getName());
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§cUsage: /party accept <leader>");
            return;
        }

        Player leader = Bukkit.getPlayerExact(args[1]);
        if (leader == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        if (!partyManager.hasPendingInvite(player, leader)) {
            player.sendMessage("§cYou have no pending invite from that player.");
            return;
        }

        Party leaderParty = partyManager.getParty(leader);
        if (leaderParty != null && partyManager.isFull(leaderParty)) {
            player.sendMessage("§cThe party is already full.");
            return;
        }

        partyManager.acceptInvite(player, leader);
        player.sendMessage("§aYou joined " + leader.getName() + "'s party.");
        leader.sendMessage("§a" + player.getName() + " joined your party.");
    }

    private void handleLeave(Player player) {
        if (!partyManager.isInParty(player)) {
            player.sendMessage("§cYou're not in a party.");
            return;
        }

        partyManager.leaveParty(player);
        player.sendMessage("§eYou left the party.");
    }
}