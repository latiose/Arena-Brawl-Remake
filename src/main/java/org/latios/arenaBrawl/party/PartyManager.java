
package org.latios.arenaBrawl.party;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.queue.QueueManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {

    public static final int MAX_PARTY_SIZE = 2;

    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private QueueManager queueManager;

    public Party getParty(Player player) {
        return parties.get(player.getUniqueId());
    }

    public void setQueueManager(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public boolean isInParty(Player player) {
        return parties.containsKey(player.getUniqueId());
    }

    public Party createParty(Player leader) {
        if (isInParty(leader)) return getParty(leader);
        Party party = new Party(leader);
        parties.put(leader.getUniqueId(), party);
        return party;
    }

    public void invite(Player leader, Player target) {
        pendingInvites.put(target.getUniqueId(), leader.getUniqueId());
    }

    public boolean hasPendingInvite(Player target, Player leader) {
        UUID inviter = pendingInvites.get(target.getUniqueId());
        return inviter != null && inviter.equals(leader.getUniqueId());
    }

    public void acceptInvite(Player target, Player leader) {
        pendingInvites.remove(target.getUniqueId());

        if (isInParty(target)) return;

        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            party = createParty(leader);
        }

        if (isFull(party)) return;
        party.addMember(target);
        parties.put(target.getUniqueId(), party);
        queueManager.handlePartyChange(party);
    }

    public void leaveParty(Player player) {
        Party party = getParty(player);
        if (party == null) return;

        party.removeMember(player);
        parties.remove(player.getUniqueId());

        if (party.isLeader(player)) {
            for (UUID memberId : party.getMembers()) {
                parties.remove(memberId);
            }
        }
    }

    public boolean isLeader(Player player) {
        Party party = getParty(player);
        return party.isLeader(player);
    }
    public boolean isFull(Party party) {
        return party.size() >= MAX_PARTY_SIZE;
    }
}