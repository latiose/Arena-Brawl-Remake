package org.latios.arenaBrawl.game;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

public class Game {
    private final List<Player> red;
    private final List<Player> blue;
    private final Scoreboard scoreboard;

    public Game(List<Player> red, List<Player> blue, Scoreboard scoreboard) {
        this.red = red;
        this.blue = blue;
        this.scoreboard = scoreboard;
    }

    public List<Player> getRed() { return red; }
    public List<Player> getBlue() { return blue; }
    public Scoreboard getScoreboard() { return scoreboard; }

    public List<Player> getAllPlayers() {
        return java.util.stream.Stream.concat(red.stream(), blue.stream()).toList();
    }
}
