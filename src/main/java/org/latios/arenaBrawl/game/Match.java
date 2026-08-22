
package org.latios.arenaBrawl.game;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.latios.arenaBrawl.powerups.PowerupManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Match {

    private final List<Player> red;
    private final List<Player> blue;
    private Scoreboard scoreboard;
    private final List<Player> aliveRed;
    private final List<Player> aliveBlue;
    private final long startedAt;
    private final PowerupManager powerupManager = new PowerupManager();
    private boolean doubleDamageActive = false;
    private final Map<Player, org.bukkit.scoreboard.Scoreboard> individualScoreboards = new HashMap<>();

    public Match(List<Player> red, List<Player> blue) {
        this.red = red;
        this.blue = blue;
       // this.scoreboard = scoreboard;
        this.aliveRed = new java.util.ArrayList<>(red);
        this.aliveBlue = new java.util.ArrayList<>(blue);
        this.startedAt = System.currentTimeMillis();
    }

    public List<Player> getRed() { return red; }
    public List<Player> getBlue() { return blue; }
    public Scoreboard getScoreboard() { return scoreboard; }
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



    public Map<Player, Scoreboard> getIndividualScoreboards() {
        return individualScoreboards;
    }

    public void setIndividualScoreboards(Map<Player, org.bukkit.scoreboard.Scoreboard> boards) {
        individualScoreboards.putAll(boards);
    }
}