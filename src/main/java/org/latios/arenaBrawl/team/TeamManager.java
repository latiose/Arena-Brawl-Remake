
package org.latios.arenaBrawl.team;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamManager {

    private final Map<UUID, Team> playerTeams = new HashMap<>();

    public void setTeam(Player player, Team team) {
        playerTeams.put(player.getUniqueId(), team);
    }

    public Team getTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public boolean isAlly(Player a, Player b) {
        Team teamA = getTeam(a);
        Team teamB = getTeam(b);
        return teamA != null && teamA == teamB;
    }

    public boolean isEnemy(Player a, Player b) {
        Team teamA = getTeam(a);
        Team teamB = getTeam(b);
        return teamA != null && teamB != null && teamA != teamB;
    }

    public void clear(Player player) {
        playerTeams.remove(player.getUniqueId());
    }

    public void clearAll() {
        playerTeams.clear();
    }
}