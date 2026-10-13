package org.latios.arenaBrawl.queue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.game.ArenaManager;
import org.latios.arenaBrawl.game.ArenaMapManager;
import org.latios.arenaBrawl.game.MatchType;
import org.latios.arenaBrawl.party.Party;
import org.latios.arenaBrawl.party.PartyManager;
import org.latios.arenaBrawl.rating.RatingManager;

import java.util.*;

public class QueueManager implements Listener {
    private static final int INITIAL_RATING_RANGE = 100;
    private static final int RATING_RANGE_INCREMENT = 100;
    private static final int WIDE_SEARCH_STEP = 6;
    private static final long RANGE_INCREMENT_MILLIS = 10_000L;

    private static final long NON_RATED_UPDATE_INTERVAL = 10 * 60 * 1000L;
    private long lastNonRatedUpdateSent = System.currentTimeMillis();

    private final Map<MatchType, Set<UUID>> queuedPlayers = new EnumMap<>(MatchType.class);
    private final Map<UUID, RatedEntry> ratedEntries = new LinkedHashMap<>();
    private final PartyManager partyManager;
    private final RatingManager ratingManager;
    private final ArenaManager arenaManager;
    private final ArenaMapManager arenaMapManager;
    private boolean ratedTeamsEnabled = false;

    public QueueManager(Plugin plugin, PartyManager partyManager, RatingManager ratingManager,
                        ArenaManager arenaManager, ArenaMapManager arenaMapManager) {
        this.partyManager = partyManager;
        this.ratingManager = ratingManager;
        this.arenaManager = arenaManager;
        this.arenaMapManager = arenaMapManager;
        for (MatchType type : MatchType.values()) queuedPlayers.put(type, new LinkedHashSet<>());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                updateRatedSearch();
                checkAndSendPeriodicQueueUpdates();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void handlePartyChange(Party party) {
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && isQueued(member)) {
                leaveQueue(member);
                member.sendMessage("§cYour party changed, you were removed from the queue.");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) { leaveQueue(event.getPlayer()); }

    public boolean isQueued(Player player) {
        return isQueued(player.getUniqueId());
    }

    public boolean isQueued(Player player, MatchType type) {
        if (type == MatchType.TEAMS && ratedEntries.values().stream()
                .anyMatch(entry -> entry.members.contains(player.getUniqueId()))) return true;
        return queuedPlayers.get(type).contains(player.getUniqueId());
    }

    public int joinQueue(Player player, MatchType type) {
        if (isQueued(player)) return -1;

        Party party = partyManager.getParty(player);
        List<UUID> members = new ArrayList<>();

        if (party != null) {
            if (!partyManager.isLeader(player)) {
                player.sendMessage("§cOnly the party leader can queue the party!");
                return -1;
            }
            if (party.size() > type.getTeamSize()) {
                player.sendMessage("§cYour party is too large for this match type!");
                return -1;
            }
            members.addAll(party.getMembers());
        } else {
            members.add(player.getUniqueId());
        }

        if (members.stream().anyMatch(this::isQueued)) return -1;

        if (type == MatchType.TEAMS && ratedTeamsEnabled) {
            int maxStep = ratedEntries.values().stream().mapToInt(e -> e.searchStep).max().orElse(0);
            RatedEntry entry = new RatedEntry(player.getUniqueId(), members, averageRating(members), maxStep);
            ratedEntries.put(entry.leaderId, entry);
            sendRatedSearchMessage(entry);
            tryStartMatches();
            return getQueueSize(type);
        }

        Set<UUID> queue = queuedPlayers.get(type);
        queue.addAll(members);

        int queueSize = queue.size();
        for (UUID memberId : members) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null && p.isOnline()) {
                p.sendMessage("§eThere are currently " + queueSize + " players in the queue.");
            }
        }

        tryStartMatches();
        return queue.size();
    }

    public int joinQueue(Player player) {
        return joinQueue(player, MatchType.TEAMS);
    }

    private boolean isQueued(UUID playerId) {
        return queuedPlayers.values().stream().anyMatch(queue -> queue.contains(playerId))
                || ratedEntries.values().stream().anyMatch(entry -> entry.members.contains(playerId));
    }

    public void leaveQueue(Player player) {
        queuedPlayers.values().forEach(queue -> removePartyOrPlayer(queue, player));
        ratedEntries.entrySet().removeIf(entry -> entry.getValue().members.contains(player.getUniqueId()));
    }

    public void leaveQueue(Player player, MatchType type) {
        if (type == MatchType.TEAMS) {
            ratedEntries.entrySet().removeIf(entry -> entry.getValue().members.contains(player.getUniqueId()));
        }
        removePartyOrPlayer(queuedPlayers.get(type), player);
    }

    private void removePartyOrPlayer(Set<UUID> queue, Player player) {
        Party party = partyManager.getParty(player);
        if (party != null) party.getMembers().forEach(queue::remove);
        else queue.remove(player.getUniqueId());
    }

    public int getQueueSize() {
        return queuedPlayers.values().stream().mapToInt(Set::size).sum()
                + ratedEntries.values().stream().mapToInt(entry -> entry.members.size()).sum();
    }

    public int getQueueSize(MatchType type) {
        if (type == MatchType.TEAMS) {
            return queuedPlayers.get(type).size()
                    + ratedEntries.values().stream().mapToInt(entry -> entry.members.size()).sum();
        }
        return queuedPlayers.get(type).size();
    }

    public boolean isRatedTeamsEnabled() {
        return ratedTeamsEnabled;
    }

    public void setRatedTeamsEnabled(boolean enabled) {
        if (ratedTeamsEnabled == enabled) return;
        ratedTeamsEnabled = enabled;
        if (enabled) {
            Set<UUID> oldQueue = queuedPlayers.get(MatchType.TEAMS);
            Set<UUID> considered = new HashSet<>();
            for (UUID playerId : new ArrayList<>(oldQueue)) {
                if (!considered.add(playerId)) continue;
                Player player = Bukkit.getPlayer(playerId);
                if (player == null) continue;
                Party party = partyManager.getParty(player);
                Collection<UUID> members = party == null ? List.of(playerId) : party.getMembers();
                if (members.stream().allMatch(oldQueue::contains)) {
                    int maxStep = ratedEntries.values().stream().mapToInt(e -> e.searchStep).max().orElse(0);
                    RatedEntry entry = new RatedEntry(playerId, members, averageRating(members), maxStep);
                    ratedEntries.put(entry.leaderId, entry);
                    considered.addAll(members);
                    oldQueue.removeAll(members);
                }
            }
        } else {
            for (RatedEntry entry : ratedEntries.values()) {
                queuedPlayers.get(MatchType.TEAMS).addAll(entry.members);
            }
            ratedEntries.clear();
        }
        tryStartMatches();
    }

    private void checkAndSendPeriodicQueueUpdates() {
        long now = System.currentTimeMillis();
        if (now - lastNonRatedUpdateSent >= NON_RATED_UPDATE_INTERVAL) {
            lastNonRatedUpdateSent = now;
            sendQueueUpdates();
        }
    }

    private void sendQueueUpdates() {
        for (Map.Entry<MatchType, Set<UUID>> entry : queuedPlayers.entrySet()) {
            if (entry.getKey() == MatchType.TEAMS && ratedTeamsEnabled) continue;
            int queueSize = getQueueSize(entry.getKey());
            for (UUID playerId : entry.getValue()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage("§eThere are currently " + queueSize + " players in the queue.");
                }
            }
        }
    }

    private void tryStartMatches() {
          for (MatchType type : MatchType.values()) {
            if (type == MatchType.TEAMS && ratedTeamsEnabled) {
                tryStartRatedMatches();
                continue;
            }
            Set<UUID> queue = queuedPlayers.get(type);
            if (queue.size() < type.playersNeeded() || arenaMapManager.getAvailableMapCount() == 0) continue;

            boolean fifo = type == MatchType.TEAMS;

            List<List<Player>> groups = new ArrayList<>();
            int total = 0;
            Set<UUID> considered = new HashSet<>();
            List<UUID> queueOrder = new ArrayList<>(queue);
            if (!fifo) {
                queueOrder.sort((first, second) -> Boolean.compare(isSoloPlayer(second), isSoloPlayer(first)));
            }


            for (UUID id : queueOrder) {
                if (!considered.add(id)) continue;
                Player queuedPlayer = Bukkit.getPlayer(id);
                if (queuedPlayer == null || !queuedPlayer.isOnline()) continue;
                Party party = partyManager.getParty(queuedPlayer);
                Collection<UUID> group = party == null ? List.of(id) : party.getMembers();
                considered.addAll(group);
                if (total + group.size() > type.playersNeeded()
                        || group.stream().anyMatch(memberId -> !queue.contains(memberId)
                        || Bukkit.getPlayer(memberId) == null || !Bukkit.getPlayer(memberId).isOnline())) {
                    continue;
                }
                groups.add(group.stream().map(Bukkit::getPlayer).toList());
                total += group.size();
                if (total == type.playersNeeded()) break;
            }
              if (total < type.playersNeeded()) continue;

              List<Player> players = fifo
                      ? arrangeByEntryOrder(groups, type.getTeamSize())
                      : arrangeForType(type, groups);
              if (players == null) continue;
              players.forEach(p -> queue.remove(p.getUniqueId()));
              arenaManager.startMatch(type, players);
        }
    }

    private List<Player> arrangeByEntryOrder(List<List<Player>> groups, int teamSize) {
        int n = groups.size();

        for (int mask = 1; mask < (1 << n); mask += 2) {
            int sizeA = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) sizeA += groups.get(i).size();
            }
            if (sizeA != teamSize) continue;

            List<Player> ordered = new ArrayList<>();
            for (int i = 0; i < n; i++) if ((mask & (1 << i)) != 0) ordered.addAll(groups.get(i));
            for (int i = 0; i < n; i++) if ((mask & (1 << i)) == 0) ordered.addAll(groups.get(i));
            return ordered;
        }
        return null;
    }

    private void updateRatedSearch() {
        if (!ratedTeamsEnabled || ratedEntries.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (RatedEntry entry : ratedEntries.values()) {
            int previousStep = entry.searchStep;
            entry.searchStep = Math.min(WIDE_SEARCH_STEP,
                    (int) ((now - entry.joinedAt) / RANGE_INCREMENT_MILLIS));

            if (entry.searchStep >= WIDE_SEARCH_STEP) {
                if (now - entry.lastWideMessageSent >= 10_000L) {
                    entry.lastWideMessageSent = now;
                    sendRatedSearchMessage(entry);
                }
            } else if (entry.searchStep > previousStep) {
                sendRatedSearchMessage(entry);
            }
        }
        tryStartRatedMatches();
    }

    private void sendRatedSearchMessage(RatedEntry entry) {
        String message;
        if (entry.searchStep >= WIDE_SEARCH_STEP) {
            message = "§eUsing a {WIDE SEARCH} to try and find you a match";
        } else {
            int minimum = (int) Math.floor(entry.averageRating - entry.getRange());
            int maximum = (int) Math.ceil(entry.averageRating + entry.getRange());
            message = "§eCurrent search parameters {" + minimum + " - " + maximum + "}";
        }

        for (UUID memberId : entry.members) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    private void tryStartRatedMatches() {
        while (arenaMapManager.getAvailableMapCount() > 0) {
            ratedEntries.values().removeIf(entry -> {
                if (isEntryValid(entry)) return false;
                for (Player p : onlinePlayers(entry)) {
                    p.sendMessage("§cYour party changed, you were removed from the queue.");
                }
                return true;
            });

            List<RatedEntry> entries = new ArrayList<>(ratedEntries.values());
            entries.removeIf(entry -> onlinePlayers(entry).size() != entry.members.size());

            List<RatedEntry> selected = findRatedMatch(entries);
            if (selected == null) return;

            List<List<Player>> groups = selected.stream().map(this::onlinePlayers).toList();
            List<Player> players = arrangeForType(MatchType.TEAMS, groups);
            if (players == null) return;

            selected.forEach(entry -> ratedEntries.remove(entry.leaderId));
            arenaManager.startMatch(MatchType.TEAMS, players);
        }
    }

    private List<RatedEntry> findRatedMatch(List<RatedEntry> entries) {
        List<RatedEntry> byWait = new ArrayList<>(entries);
        byWait.sort(Comparator.comparingLong(e -> e.joinedAt));

        for (RatedEntry anchor : byWait) {
            List<RatedEntry> candidates = new ArrayList<>(entries);
            candidates.remove(anchor);
            candidates.sort(Comparator.comparingDouble(e -> Math.abs(e.averageRating - anchor.averageRating)));

            List<RatedEntry> selected = new ArrayList<>();
            selected.add(anchor);
            List<RatedEntry> result = searchRated(candidates, 0, selected, anchor.members.size());
            if (result != null) return result;
        }
        return null;
    }

    private List<RatedEntry> searchRated(List<RatedEntry> candidates, int index,
                                         List<RatedEntry> selected, int playerCount) {
        int needed = MatchType.TEAMS.playersNeeded();
        if (playerCount == needed) {
            if (!isCompatible(selected)) return null;
            List<List<Player>> groups = selected.stream().map(this::onlinePlayers).toList();
            return arrangeForType(MatchType.TEAMS, groups) != null ? new ArrayList<>(selected) : null;
        }
        for (int i = index; i < candidates.size(); i++) {
            RatedEntry entry = candidates.get(i);
            if (playerCount + entry.members.size() > needed) continue;
            selected.add(entry);
            List<RatedEntry> result = searchRated(candidates, i + 1, selected, playerCount + entry.members.size());
            selected.removeLast();
            if (result != null) return result;
        }
        return null;
    }

    private boolean isCompatible(List<RatedEntry> entries) {
        boolean hasWideSearch = entries.stream().anyMatch(entry -> entry.searchStep >= WIDE_SEARCH_STEP);
        if (hasWideSearch) {
            return true;
        }

        double minimum = entries.stream().mapToDouble(RatedEntry::getRange).min().orElse(0);
        double lowest = entries.stream().mapToDouble(entry -> entry.averageRating).min().orElse(0);
        double highest = entries.stream().mapToDouble(entry -> entry.averageRating).max().orElse(0);
        return highest - lowest <= minimum;
    }

    private double averageRating(Collection<UUID> members) {
        return members.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .mapToDouble(ratingManager::getRating)
                .average()
                .orElse(1000.0);
    }

    private final class RatedEntry {
        private final UUID leaderId;
        private final List<UUID> members;
        private final double averageRating;
        private final long joinedAt;
        private int searchStep;
        private long lastWideMessageSent;

        private RatedEntry(UUID leaderId, Collection<UUID> members, double averageRating, int initialStep) {
            this.leaderId = leaderId;
            this.members = new ArrayList<>(members);
            this.averageRating = averageRating;
            this.searchStep = initialStep;
            this.joinedAt = System.currentTimeMillis() - (initialStep * RANGE_INCREMENT_MILLIS);
            this.lastWideMessageSent = System.currentTimeMillis();
        }

        private int getRange() {
            return INITIAL_RATING_RANGE + (RATING_RANGE_INCREMENT * searchStep);
        }
    }

    private boolean isSoloPlayer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player == null || partyManager.getParty(player) == null;
    }

    private static final double TEAMMATE_CLOSENESS_WEIGHT = 1.0;

    private List<Player> arrangeTeams(List<List<Player>> groups, List<Double> ratings, int teamSize) {
        int n = groups.size();
        int bestMask = -1;
        double bestScore = Double.MAX_VALUE;

        for (int mask = 1; mask < (1 << n); mask += 2) {
            int sizeA = 0, sizeB = 0;
            double sumA = 0, sumB = 0;
            double minA = Double.MAX_VALUE, maxA = -Double.MAX_VALUE;
            double minB = Double.MAX_VALUE, maxB = -Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                int size = groups.get(i).size();
                double r = ratings.get(i);
                if ((mask & (1 << i)) != 0) {
                    sizeA += size; sumA += r * size;
                    minA = Math.min(minA, r); maxA = Math.max(maxA, r);
                } else {
                    sizeB += size; sumB += r * size;
                    minB = Math.min(minB, r); maxB = Math.max(maxB, r);
                }
            }
            if (sizeA != teamSize || sizeB != teamSize) continue;

            double teamDiff = Math.abs(sumA / sizeA - sumB / sizeB);
            double spread = (maxA - minA) + (maxB - minB);
            double score = teamDiff + (TEAMMATE_CLOSENESS_WEIGHT * spread);

            if (score < bestScore) { bestScore = score; bestMask = mask; }
        }
        if (bestMask < 0) return null;

        List<Player> ordered = new ArrayList<>();
        for (int i = 0; i < n; i++) if ((bestMask & (1 << i)) != 0) ordered.addAll(groups.get(i));
        for (int i = 0; i < n; i++) if ((bestMask & (1 << i)) == 0) ordered.addAll(groups.get(i));
        return ordered;
    }

    private List<Player> arrangeForType(MatchType type, List<List<Player>> groups) {
        if (type.playersNeeded() != type.getTeamSize() * 2) {
            List<Player> flat = new ArrayList<>();
            groups.forEach(flat::addAll);
            return flat;
        }
        List<Double> ratings = groups.stream()
                .map(g -> averageRating(g.stream().map(Player::getUniqueId).toList()))
                .toList();
        return arrangeTeams(groups, ratings, type.getTeamSize());
    }

    private List<Player> onlinePlayers(RatedEntry entry) {
        return entry.members.stream().map(Bukkit::getPlayer)
                .filter(Objects::nonNull).toList();
    }

    private boolean isEntryValid(RatedEntry entry) {
        Player leader = Bukkit.getPlayer(entry.leaderId);
        if (leader == null) return false;
        Party party = partyManager.getParty(leader);
        Set<UUID> current = party == null ? Set.of(entry.leaderId) : new HashSet<>(party.getMembers());
        return current.equals(new HashSet<>(entry.members));
    }
}