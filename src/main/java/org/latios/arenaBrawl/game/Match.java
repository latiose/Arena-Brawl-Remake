
package org.latios.arenaBrawl.game;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.latios.arenaBrawl.powerups.PowerupManager;

import java.util.*;
import java.util.stream.Stream;

public class Match {

    private final List<Player> red;
    private final List<Player> blue;
    private final List<Player> aliveRed;
    private final List<Player> aliveBlue;
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
    public Match(List<Player> red, List<Player> blue, ArenaMap arenaMap) {
        this.red = red;
        this.blue = blue;
        this.arenaMap = arenaMap;
        this.aliveRed = new ArrayList<>(red);
        this.aliveBlue = new ArrayList<>(blue);
        this.startedAt = System.currentTimeMillis();
    }

    public ArenaMap getArenaMap() { return arenaMap; }
    public List<Player> getRed() { return red; }
    public List<Player> getBlue() { return blue; }

    public long getStartedAt() { return startedAt; }
    public PowerupManager getPowerupManager() { return powerupManager; }
    public boolean isDoubleDamageActive() { return doubleDamageActive; }
    public void setDoubleDamageActive(boolean active) { this.doubleDamageActive = active; }

    public List<Player> getAllPlayers() {
        return Stream.concat(red.stream(), blue.stream()).toList();
    }

    public boolean contains(Player player) {
        return red.contains(player) || blue.contains(player);
    }

    public String eliminate(Player player) {
        aliveRed.remove(player);
        aliveBlue.remove(player);

        if (aliveRed.isEmpty()) return "BLUE";
        if (aliveBlue.isEmpty()) return "RED";
        return null;
    }

    public void refreshPlayerReference(Player freshPlayer) {
        UUID id = freshPlayer.getUniqueId();

        replaceInList(red, id, freshPlayer);
        replaceInList(blue, id, freshPlayer);
        replaceInList(aliveRed, id, freshPlayer);
        replaceInList(aliveBlue, id, freshPlayer);

        Scoreboard existingBoard = null;
        var iterator = individualScoreboards.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().getUniqueId().equals(id)) {
                existingBoard = entry.getValue();
                iterator.remove();
            }
        }
        if (existingBoard != null) {
            individualScoreboards.put(freshPlayer, existingBoard);
        }
    }

    private void replaceInList(List<Player> list, UUID id, Player freshPlayer) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getUniqueId().equals(id)) {
                list.set(i, freshPlayer);
                return;
            }
        }
    }

    public void applyScoreboardTo(Player player) {
        Scoreboard board = individualScoreboards.get(player);
        if (board != null) {
            player.setScoreboard(board);
        }
    }

    public Map<Player, Scoreboard> getIndividualScoreboards() {
        return individualScoreboards;
    }

    public void setIndividualScoreboards(Map<Player, org.bukkit.scoreboard.Scoreboard> boards) {
        individualScoreboards.putAll(boards);
    }
}