
package org.latios.arenaBrawl.party;

import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Party {

    private final UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Party(Player leader) {
        this.leader = leader.getUniqueId();
        this.members.add(leader.getUniqueId());
    }

    public UUID getLeader() { return leader; }
    public Set<UUID> getMembers() { return members; }

    public boolean isLeader(Player player) {
        return leader.equals(player.getUniqueId());
    }

    public void addMember(Player player) {
        members.add(player.getUniqueId());
    }

    public void removeMember(Player player) {
        members.remove(player.getUniqueId());
    }

    public int size() {
        return members.size();
    }
}