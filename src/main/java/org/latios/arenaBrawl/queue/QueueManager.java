package org.latios.arenaBrawl.queue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.game.ArenaManager;
import org.latios.arenaBrawl.game.ArenaMapManager;
import org.latios.arenaBrawl.game.MatchType;
import org.latios.arenaBrawl.party.Party;
import org.latios.arenaBrawl.party.PartyManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QueueManager implements Listener {
    private final Map<MatchType, Set<UUID>> queuedPlayers = new EnumMap<>(MatchType.class);
    private final PartyManager partyManager;
    private final ArenaManager arenaManager;
    private final ArenaMapManager arenaMapManager;

    public QueueManager(Plugin plugin, PartyManager partyManager, ArenaManager arenaManager, ArenaMapManager arenaMapManager) {
        this.partyManager = partyManager;
        this.arenaManager = arenaManager;
        this.arenaMapManager = arenaMapManager;
        for (MatchType type : MatchType.values()) queuedPlayers.put(type, new LinkedHashSet<>());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) { leaveQueue(event.getPlayer()); }

    public boolean isQueued(Player player) {
        return queuedPlayers.values().stream().anyMatch(queue -> queue.contains(player.getUniqueId()));
    }

    public boolean isQueued(Player player, MatchType type) {
        return queuedPlayers.get(type).contains(player.getUniqueId());
    }

    public int joinQueue(Player player, MatchType type) {
        if (isQueued(player)) return -1;
        Party party = partyManager.getParty(player);
        List<UUID> members = new ArrayList<>();
        if (party != null && partyManager.isLeader(player)) {
            if (party.size() > type.getTeamSize()) return -1;
            members.addAll(party.getMembers());
        } else if (party != null) {
            return -1;
        } else {
            members.add(player.getUniqueId());
        }

        Set<UUID> queue = queuedPlayers.get(type);
        if (members.stream().anyMatch(this::isQueued)) return -1;
        queue.addAll(members);
        tryStartMatches();
        return queue.size();
    }

    public int joinQueue(Player player) {
        return joinQueue(player, MatchType.TEAMS);
    }

    private boolean isQueued(UUID playerId) {
        return queuedPlayers.values().stream().anyMatch(queue -> queue.contains(playerId));
    }

    public void leaveQueue(Player player) {
        queuedPlayers.values().forEach(queue -> removePartyOrPlayer(queue, player));
    }

    public void leaveQueue(Player player, MatchType type) {
        removePartyOrPlayer(queuedPlayers.get(type), player);
    }

    private void removePartyOrPlayer(Set<UUID> queue, Player player) {
        Party party = partyManager.getParty(player);
        if (party != null) party.getMembers().forEach(queue::remove);
        else queue.remove(player.getUniqueId());
    }

    public int getQueueSize() { return queuedPlayers.values().stream().mapToInt(Set::size).sum(); }
    public int getQueueSize(MatchType type) { return queuedPlayers.get(type).size(); }

    private void tryStartMatches() {
        for (MatchType type : MatchType.values()) {
            Set<UUID> queue = queuedPlayers.get(type);
            if (queue.size() < type.playersNeeded() || arenaMapManager.getAvailableMapCount() == 0) continue;
            List<Player> players = new ArrayList<>();
            List<UUID> selected = new ArrayList<>();
            Set<UUID> considered = new HashSet<>();
            for (UUID id : queue) {
                if (!considered.add(id)) continue;
                Player queuedPlayer = Bukkit.getPlayer(id);
                if (queuedPlayer == null || !queuedPlayer.isOnline()) continue;
                Party party = partyManager.getParty(queuedPlayer);
                Collection<UUID> group = party == null ? List.of(id) : party.getMembers();
                considered.addAll(group);
                if (players.size() + group.size() > type.playersNeeded()
                        || group.stream().anyMatch(memberId -> !queue.contains(memberId)
                        || Bukkit.getPlayer(memberId) == null || !Bukkit.getPlayer(memberId).isOnline())) {
                    continue;
                }
                List<Player> groupPlayers = group.stream().map(Bukkit::getPlayer).toList();
                selected.addAll(group);
                players.addAll(groupPlayers);
                if (players.size() == type.playersNeeded()) break;
            }
            if (players.size() < type.playersNeeded()) {
                selected.forEach(queue::remove);
                continue;
            }
            selected.forEach(queue::remove);
            arenaManager.startMatch(type, players);
        }
    }
}
