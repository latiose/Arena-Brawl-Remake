// game/Match.java
package org.latios.arenaBrawl.game;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Match {

    private final List<Player> red;
    private final List<Player> blue;
    private final Scoreboard scoreboard;
    private final List<Player> aliveRed;
    private final List<Player> aliveBlue;

    public Match(List<Player> red, List<Player> blue, Scoreboard scoreboard) {
        this.red = red;
        this.blue = blue;
        this.scoreboard = scoreboard;
        this.aliveRed = new ArrayList<>(red);
        this.aliveBlue = new ArrayList<>(blue);
    }

    public List<Player> getRed() { return red; }
    public List<Player> getBlue() { return blue; }
    public Scoreboard getScoreboard() { return scoreboard; }

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
}