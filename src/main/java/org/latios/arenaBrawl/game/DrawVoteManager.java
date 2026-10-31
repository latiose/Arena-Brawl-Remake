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
        if (match == null || match.isEnded()) {
            return VoteResult.MATCH_ENDED;
        }
        if (match.isEliminated(player)) {
            return VoteResult.ELIMINATED;
        }

        Set<UUID> votes = votesByMatch.computeIfAbsent(match, k -> new HashSet<>());

        if (!votes.add(player.getUniqueId())) {
            return VoteResult.ALREADY_VOTED;
        }

        if (votes.size() >= getRequiredVotes(match)) {
            votesByMatch.remove(match);
            return VoteResult.UNANIMOUS;
        }

        return VoteResult.RECORDED;
    }

    /**
     * Number of votes needed for the draw to pass: every player still alive in
     * the match. Each death or disconnect therefore lowers it by one.
     */
    public int getRequiredVotes(Match match) {
        return Math.max(1, match.getAliveCount());
    }

    public int getVoteCount(Match match) {
        return votesByMatch.getOrDefault(match, Set.of()).size();
    }

    /** Drops every vote for the match, e.g. after a death or disconnect. */
    public void reset(Match match) {
        votesByMatch.remove(match);
    }

    /** Call when a match ends for any other reason, to avoid leaking vote state. */
    public void clear(Match match) {
        reset(match);
    }

    public enum VoteResult {
        RECORDED, ALREADY_VOTED, UNANIMOUS, ELIMINATED, MATCH_ENDED
    }
}
