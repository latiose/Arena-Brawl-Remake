// queue/QueueManager.java
package org.latios.arenaBrawl.queue;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.game.ArenaManager;
import org.latios.arenaBrawl.party.Party;
import org.latios.arenaBrawl.party.PartyManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class QueueManager {

    private static final int MATCH_SIZE = 4;

    private final Set<UUID> queuedPlayers = new LinkedHashSet<>();
    private final PartyManager partyManager;
    private final ArenaManager arenaManager;

    public QueueManager(PartyManager partyManager, ArenaManager arenaManager) {
        this.partyManager = partyManager;
        this.arenaManager = arenaManager;
    }

    public boolean isQueued(Player player) {
        return queuedPlayers.contains(player.getUniqueId());
    }

    public boolean joinQueue(Player player) {
        Party party = partyManager.getParty(player);
        List<UUID> toQueue = new ArrayList<>();

        if (party != null && partyManager.isLeader(player)) {
            for (UUID memberId : party.getMembers()) {
                if (queuedPlayers.contains(memberId)) return false;
                toQueue.add(memberId);
            }
        } else {
            if (queuedPlayers.contains(player.getUniqueId())) return false;
            toQueue.add(player.getUniqueId());
        }

        queuedPlayers.addAll(toQueue);
        tryStartMatch();
        return true;
    }

    public void leaveQueue(Player player) {
        Party party = partyManager.getParty(player);
        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                queuedPlayers.remove(memberId);
            }
        } else {
            queuedPlayers.remove(player.getUniqueId());
        }
    }

    public int getQueueSize() {
        return queuedPlayers.size();
    }

    private void tryStartMatch() {
        if (queuedPlayers.size() < MATCH_SIZE) return;

        List<UUID> selected = new ArrayList<>();
        for (UUID id : queuedPlayers) {
            selected.add(id);
            if (selected.size() == MATCH_SIZE) break;
        }

        List<Player> players = selected.stream()
                .map(org.bukkit.Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .toList();

        if (players.size() < MATCH_SIZE) {
            queuedPlayers.removeAll(selected);
            return;
        }

        queuedPlayers.removeAll(selected);

        List<Player> team1 = new ArrayList<>();
        List<Player> team2 = new ArrayList<>();
        assignTeams(players, team1, team2);

        arenaManager.startMatch(team1.get(0), team1.get(1), team2.get(0), team2.get(1));
    }


    private void assignTeams(List<Player> players, List<Player> team1, List<Player> team2) {
        for (Player player : players) {
            Party party = partyManager.getParty(player);

            if (party != null && party.size() == 2) {
                Player partner = players.stream()
                        .filter(p -> !p.equals(player) && party.getMembers().contains(p.getUniqueId()))
                        .findFirst()
                        .orElse(null);

                if (partner != null && !team1.contains(player) && !team1.contains(partner)
                        && !team2.contains(player) && !team2.contains(partner)) {
                    if (team1.size() <= team2.size() && team1.size() + 2 <= 2) {
                        team1.add(player);
                        team1.add(partner);
                    } else {
                        team2.add(player);
                        team2.add(partner);
                    }
                    continue;
                }
            }

            if (team1.contains(player) || team2.contains(player)) continue;

            if (team1.size() < 2) {
                team1.add(player);
            } else {
                team2.add(player);
            }
        }
    }
}