package org.latios.arenaBrawl.game;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DrawVoteManager {

    private final Map<Match, Set<UUID>> votesByMatch = new HashMap<>();

    /**
     * Registers a vote from the player for their current match.
     * Returns the result of the vote attempt.
     */
    public VoteResult registerVote(Player player, Match match) {
        Set<UUID> votes = votesByMatch.computeIfAbsent(match, k -> new HashSet<>());

        if (!votes.add(player.getUniqueId())) {
            return VoteResult.ALREADY_VOTED;
        }

        int totalPlayers = match.getAllPlayers().size();
        int votesCast = votes.size();

        if (votesCast >= totalPlayers) {
            votesByMatch.remove(match);
            return VoteResult.UNANIMOUS;
        }

        return VoteResult.RECORDED;
    }

    public int getVoteCount(Match match) {
        return votesByMatch.getOrDefault(match, Set.of()).size();
    }

    /** Call when a match ends for any other reason, to avoid leaking vote state. */
    public void clear(Match match) {
        votesByMatch.remove(match);
    }

    public enum VoteResult {
        RECORDED, ALREADY_VOTED, UNANIMOUS
    }
}