// game/MatchManager.java
package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.abilities.AbilityManager;

import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.HungerManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.ScoreboardManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MatchManager {

    private final PlayerHealthManager healthManager;
    private final TeamManager teamManager;
    private final AbilityManager abilityManager;
    private final EnergyManager energyManager;
    private final HungerManager hungerManager;
    private final ScoreboardManager scoreboardManager;
    private final Location lobbySpawn;

    private final Map<UUID, Match> activeMatches = new HashMap<>();
    private final Map<Match, BukkitTask> scoreboardTasks = new HashMap<>();

    public MatchManager(PlayerHealthManager healthManager, TeamManager teamManager, AbilityManager abilityManager,
                        EnergyManager energyManager, HungerManager hungerManager,
                        ScoreboardManager scoreboardManager, Location lobbySpawn) {
        this.healthManager = healthManager;
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
        this.energyManager = energyManager;
        this.hungerManager = hungerManager;
        this.scoreboardManager = scoreboardManager;
        this.lobbySpawn = lobbySpawn;
    }

    public void registerMatch(Match match, BukkitTask scoreboardTask) {
        for (Player player : match.getAllPlayers()) {
            activeMatches.put(player.getUniqueId(), match);
        }
        scoreboardTasks.put(match, scoreboardTask);
    }

    public void onPlayerEliminated(Player player) {
        Match match = activeMatches.get(player.getUniqueId());
        if (match == null) return;

        player.setGameMode(org.bukkit.GameMode.SPECTATOR);

        String winner = match.eliminate(player);
        if (winner != null) {
            endMatch(match, winner);
        }
    }

    private void endMatch(Match match, String winnerTeam) {
        String winnerMessage = winnerTeam.equals("RED") ? "§cRead team" : "§9Blue team";

        for (Player player : match.getAllPlayers()) {
            player.sendTitle(winnerMessage + " §fhas won!", "", 10, 60, 10);
        }

        BukkitTask task = scoreboardTasks.remove(match);
        if (task != null) task.cancel();

        for (Player player : match.getAllPlayers()) {
            cleanupPlayer(player);
        }
    }

    private void cleanupPlayer(Player player) {
        activeMatches.remove(player.getUniqueId());
        teamManager.clear(player);
        if (!player.isOnline()) return;
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0f);
        player.teleport(lobbySpawn);
    }

    public boolean isInMatch(Player player) {
        return activeMatches.containsKey(player.getUniqueId());
    }
}