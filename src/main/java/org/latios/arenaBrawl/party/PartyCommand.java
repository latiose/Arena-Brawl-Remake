// party/PartyCommand.java
package org.latios.arenaBrawl.party;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyCommand implements CommandExecutor {

    private final PartyManager partyManager;

    public PartyCommand(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players allowed.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUse: /party <invite|accept|leave> [player]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "leave" -> handleLeave(player);
            default -> player.sendMessage("§cUnknown command.");
        }

        return true;
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§cUse: /party invite <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou cannot invite yourself.");
            return;
        }

        Party party = partyManager.getParty(player);
        if (party != null && !party.isLeader(player)) {
            player.sendMessage("§cOnly the party leader is allowed to invite other players.");
            return;
        }

        if (party != null && partyManager.isFull(party)) {
            player.sendMessage("§cYour party is already full.");
            return;
        }

        if (party == null) {
            party = partyManager.createParty(player);
        }

        partyManager.invite(player, target);
        player.sendMessage("§a" + target.getName() + " has been invited to your party");
        target.sendMessage("§e" + player.getName() + " has invited you to their party. §a/party accept " + player.getName());
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§cUse: /party accept <player>");
            return;
        }

        Player leader = Bukkit.getPlayerExact(args[1]);
        if (leader == null) {
            player.sendMessage("§cPlayer not found.");
            return;
        }

        if (!partyManager.hasPendingInvite(player, leader)) {
            player.sendMessage("§cYou have no pending invites from given player.");
            return;
        }

        Party leaderParty = partyManager.getParty(leader);
        if (leaderParty != null && partyManager.isFull(leaderParty)) {
            player.sendMessage("§cParty is already full.");
            return;
        }

        partyManager.acceptInvite(player, leader);
        player.sendMessage("§aYou have joined " + leader.getName() + "'s party.");
        leader.sendMessage("§a" + player.getName() + " has joined your party.");
    }

    private void handleLeave(Player player) {
        if (!partyManager.isInParty(player)) {
            player.sendMessage("§cYou are not in a party.");
            return;
        }

        partyManager.leaveParty(player);
        player.sendMessage("§eYou have left the party.");
    }
}