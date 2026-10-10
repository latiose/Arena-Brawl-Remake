
package org.latios.arenaBrawl.game;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.latios.arenaBrawl.powerups.PowerupManager;
import org.latios.arenaBrawl.team.Team;

import java.util.*;

public class Match {

    private final MatchType type;
    private final Map<Team, List<Player>> teams;
    private final Map<Team, List<Player>> alive = new LinkedHashMap<>();
    private final List<Player> eliminationOrder = new ArrayList<>();
    private final long startedAt;
    private final PowerupManager powerupManager = new PowerupManager();
    private boolean doubleDamageActive = false;
    private final Map<Player, org.bukkit.scoreboard.Scoreboard> individualScoreboards = new HashMap<>();

    private final ArenaMap arenaMap;
    private boolean ended = false;

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }
    public Match(MatchType type, Map<Team, List<Player>> teams, ArenaMap arenaMap) {
        this.type = type;
        this.teams = teams;
        this.arenaMap = arenaMap;
        teams.forEach((t, l) -> alive.put(t, new ArrayList<>(l)));
        this.startedAt = System.currentTimeMillis();
    }

    public MatchType getType() { return type; }
    public Map<Team, List<Player>> getTeams() { return teams; }
    public List<Player> getTeamPlayers(Team t) { return teams.getOrDefault(t, List.of()); }
    public ArenaMap getArenaMap() { return arenaMap; }
    public long getStartedAt() { return startedAt; }
    public PowerupManager getPowerupManager() { return powerupManager; }
    public boolean isDoubleDamageActive() { return doubleDamageActive; }
    public void setDoubleDamageActive(boolean active) { this.doubleDamageActive = active; }
    public Map<Player, Scoreboard> getIndividualScoreboards() { return individualScoreboards; }
    public void setIndividualScoreboards(Map<Player, Scoreboard> boards) { individualScoreboards.putAll(boards); }
    public List<Player> getRed() { return getTeamPlayers(Team.RED); }
    public List<Player> getBlue() { return getTeamPlayers(Team.BLUE); }
    public boolean contains(Player player) { return getAllPlayers().contains(player); }

    public List<Player> getAllPlayers() {
        return teams.values().stream().flatMap(List::stream).toList();
    }

    public Team eliminate(Player player) {
        eliminationOrder.add(player);
        alive.values().forEach(l -> l.remove(player));
        List<Team> remaining = alive.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(Map.Entry::getKey).toList();
        return remaining.size() == 1 ? remaining.getFirst() : null;
    }
}