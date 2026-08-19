
package org.latios.arenaBrawl.party;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {

    public static final int MAX_PARTY_SIZE = 2;

    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

    public Party getParty(Player player) {
        return parties.get(player.getUniqueId());
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

        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            party = createParty(leader);
        }

        party.addMember(target);
        parties.put(target.getUniqueId(), party);
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