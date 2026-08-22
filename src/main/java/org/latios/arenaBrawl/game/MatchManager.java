// game/MatchManager.java
package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.abilities.AbilityManager;

import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;
import org.latios.arenaBrawl.cosmetics.ArmorTier;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.*;
import org.latios.arenaBrawl.hats.HatEquipUtils;
import org.latios.arenaBrawl.hats.HatSelectionManager;
import org.latios.arenaBrawl.lobby.LobbyKit;
import org.latios.arenaBrawl.lobby.LobbyScoreboardManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.rating.RatingManager;
import org.latios.arenaBrawl.stats.StatsManager;
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
    private Match currentMatch;
    private final CooldownManager cooldownManager;
    private final UsageManager usageManager;
    private final StatsManager statsManager;
    private final LobbyScoreboardManager lobbyScoreboardManager;
    private final DamageBuffManager damageBuffManager;
    private final ArmorTierManager armorTierManager;
    private final HatSelectionManager hatSelectionManager;

    public MatchManager(PlayerHealthManager healthManager, TeamManager teamManager, AbilityManager abilityManager,
                        EnergyManager energyManager, HungerManager hungerManager,
                        ScoreboardManager scoreboardManager, Location lobbySpawn,RatingManager ratingManager,DebuffManager debuffManager,OrbitShieldManager orbitShieldManager,
                        CooldownManager cooldownManager, UsageManager usageManager, StatsManager statsManager, LobbyScoreboardManager lobbyScoreboardManager,DamageBuffManager damageBuffManager,
                        ArmorTierManager armorTierManager,HatSelectionManager hatSelectionManager) {
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
        this.cooldownManager = cooldownManager;
        this.usageManager = usageManager;
        this.statsManager = statsManager;
        this.lobbyScoreboardManager = lobbyScoreboardManager;
        this.damageBuffManager = damageBuffManager;
        this.armorTierManager = armorTierManager;
        this.hatSelectionManager = hatSelectionManager;
    }

    public void onPlayerEliminated(Player player) {
        Match match = activeMatches.get(player.getUniqueId());
        if (match == null) return;

        UUID attackerId = healthManager.getLastAttacker(player);
        if (attackerId != null) {
            Player killer = Bukkit.getPlayer(attackerId);
            if (killer != null && killer.isOnline() && !killer.equals(player)) {
                statsManager.addKill(killer);
            }
        }
        statsManager.addDeath(player);

        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.sendMessage("§cYou have been eliminated.");

        String winner = match.eliminate(player);
        if (winner != null) {
            endMatch(match, winner);
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
            loser.sendMessage(String.format("§aYour new rating is %.2f (%.2f)", ratingManager.getRating(loser), loss));
        }
    }

    private void cleanupPlayer(Player player) {
        activeMatches.remove(player.getUniqueId());
        teamManager.clear(player);
        debuffManager.clear(player);
        abilityManager.clearAbilities(player);
        damageBuffManager.clear(player);
        cooldownManager.clearPlayer(player);
        usageManager.resetPlayer(player);
        orbitShieldManager.clear(player);
        if (!player.isOnline()) return;

        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setFoodLevel(20);
        healthManager.setMaxHealth(player, 20.0);
        player.setLevel(0);
        player.setExp(0f);
        player.teleport(lobbySpawn);

        LobbyKit.giveLobbyKit(player,armorTierManager);
        lobbyScoreboardManager.show(player);
        HatEquipUtils.applyEquippedHat(player, hatSelectionManager);
    }

    public void registerMatch(Match match, BukkitTask scoreboardTask) {
        for (Player player : match.getAllPlayers()) {
            activeMatches.put(player.getUniqueId(), match);
        }
        scoreboardTasks.put(match, scoreboardTask);
        currentMatch = match;
    }

    public Match getActiveMatch() {
        return currentMatch;
    }

    private void endMatch(Match match, String winnerTeam) {
        List<Player> winners = winnerTeam.equals("RED") ? match.getRed() : match.getBlue();
        List<Player> losers = winnerTeam.equals("RED") ? match.getBlue() : match.getRed();

        applyRatingChanges(winners, losers);

        for (Player winner : winners) {
            if (winner.isOnline()) statsManager.addWin(winner);
        }
        for (Player loser : losers) {
            if (loser.isOnline()) statsManager.addLoss(loser);
        }

        String winnerMessage = winnerTeam.equals("RED") ? "§cRed Team" : "§9Blue Team";
        for (Player player : match.getAllPlayers()) {
            if (player.isOnline()) {
                player.sendTitle(winnerMessage + " §fwins!", "", 10, 60, 10);
            }
        }

        finishMatch(match);
    }

    /** Ends the match in a draw due to the 10-minute time limit. No rating changes are applied. */
    public void endMatchAsDraw(Match match) {
        for (Player player : match.getAllPlayers()) {
            if (player.isOnline()) {
                player.sendTitle("§eDraw!", "§7Time limit reached", 10, 60, 10);
            }
        }
        finishMatch(match);
    }

    private void finishMatch(Match match) {
        BukkitTask task = scoreboardTasks.remove(match);
        if (task != null) task.cancel();

        match.getPowerupManager().clear();

        if (!match.getAllPlayers().isEmpty()) {
            EntityCleanupUtils.sweepArenaEntities(match.getAllPlayers().get(0).getWorld());
        }

        if (currentMatch == match) {
            currentMatch = null;
        }

        for (Player player : match.getAllPlayers()) {
            cleanupPlayer(player);
        }
    }

    public boolean isInMatch(Player player) {
        return activeMatches.containsKey(player.getUniqueId());
    }
}