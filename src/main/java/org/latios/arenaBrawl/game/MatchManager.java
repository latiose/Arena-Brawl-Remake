// game/MatchManager.java
package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.abilities.AbilityManager;

import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.HungerManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.ScoreboardManager;
import org.latios.arenaBrawl.lobby.LobbyKit;
import org.latios.arenaBrawl.rating.RatingManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.List;
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
    private final RatingManager ratingManager;
    private final Map<UUID, Match> activeMatches = new HashMap<>();
    private final Map<Match, BukkitTask> scoreboardTasks = new HashMap<>();
    private final DebuffManager debuffManager;
    private final OrbitShieldManager orbitShieldManager;

    public MatchManager(PlayerHealthManager healthManager, TeamManager teamManager, AbilityManager abilityManager,
                        EnergyManager energyManager, HungerManager hungerManager,
                        ScoreboardManager scoreboardManager, Location lobbySpawn,RatingManager ratingManager,DebuffManager debuffManager,OrbitShieldManager orbitShieldManager) {
        this.healthManager = healthManager;
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
        this.energyManager = energyManager;
        this.hungerManager = hungerManager;
        this.scoreboardManager = scoreboardManager;
        this.lobbySpawn = lobbySpawn;
        this.ratingManager = ratingManager;
        this.debuffManager = debuffManager;
        this.orbitShieldManager = orbitShieldManager;
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
        List<Player> winners = winnerTeam.equals("RED") ? match.getRed() : match.getBlue();
        List<Player> losers = winnerTeam.equals("RED") ? match.getBlue() : match.getRed();

        applyRatingChanges(winners, losers);

        String winnerMessage = winnerTeam.equals("RED") ? "§cRed team" : "§9Blue team";

        for (Player player : match.getAllPlayers()) {
            if (player.isOnline()) {
                player.sendTitle(winnerMessage + " §fhas won!", "", 10, 60, 10);
            }
        }

        BukkitTask task = scoreboardTasks.remove(match);
        if (task != null) task.cancel();

        for (Player player : match.getAllPlayers()) {
            cleanupPlayer(player);
        }
    }

    private void applyRatingChanges(List<Player> winners, List<Player> losers) {
        double losersAvg = losers.stream().mapToDouble(ratingManager::getRating).average().orElse(1000.0);
        double winnersAvg = winners.stream().mapToDouble(ratingManager::getRating).average().orElse(1000.0);

        for (Player winner : winners) {
            if (!winner.isOnline()) continue;
            // Uses the winner's own rating against the opponent team's average
            double gain = ratingManager.calculateGain(ratingManager.getRating(winner), losersAvg);
            ratingManager.applyDelta(winner, gain);
            winner.sendMessage(String.format("§aYour new rating is %.2f (+%.2f)", ratingManager.getRating(winner), gain));
        }

        for (Player loser : losers) {
            if (!loser.isOnline()) continue;
            // Uses the loser's own rating against the opponent team's average
            double loss = ratingManager.calculateLoss(ratingManager.getRating(loser), winnersAvg);
            ratingManager.applyDelta(loser, loss);
            loser.sendMessage(String.format("§aYour new rating is %.2f (-%.2f)", ratingManager.getRating(loser), loss));
        }
    }

    private void cleanupPlayer(Player player) {
        activeMatches.remove(player.getUniqueId());
        teamManager.clear(player);
        debuffManager.clear(player);
        abilityManager.clearAbilities(player);
        orbitShieldManager.clear(player);
        if (!player.isOnline()) return;

        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setFoodLevel(20);
        healthManager.setMaxHealth(player, 20.0);
        player.setLevel(0);
        player.setExp(0f);
        player.teleport(lobbySpawn);

        LobbyKit.giveLobbyKit(player);
    }

    public boolean isInMatch(Player player) {
        return activeMatches.containsKey(player.getUniqueId());
    }
}