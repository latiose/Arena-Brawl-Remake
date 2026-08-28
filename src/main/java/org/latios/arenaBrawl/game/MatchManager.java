
package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.abilities.AbilityManager;

import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.SongOfPowerManager;
import org.latios.arenaBrawl.abilities.ultimate.BroodMotherEntityManager;
import org.latios.arenaBrawl.abilities.ultimate.UsageManager;

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

import java.util.*;

public class MatchManager {

    private final PlayerHealthManager healthManager;
    private final TeamManager teamManager;
    private final AbilityManager abilityManager;
    private final Location lobbySpawn;
    private final RatingManager ratingManager;
    private final Map<UUID, Match> activeMatches = new HashMap<>();
    private final Map<Match, BukkitTask> scoreboardTasks = new HashMap<>();
    private final DebuffManager debuffManager;
    private final OrbitShieldManager orbitShieldManager;
    private final CooldownManager cooldownManager;
    private final UsageManager usageManager;
    private final StatsManager statsManager;
    private final LobbyScoreboardManager lobbyScoreboardManager;
    private final DamageBuffManager damageBuffManager;
    private final ArmorTierManager armorTierManager;
    private final HatSelectionManager hatSelectionManager;
    private final BroodMotherEntityManager broodMotherEntityManager;
    private final Plugin plugin;
    private final ArenaMapManager arenaMapManager;
    private final List<Match> activeMatchesList = new ArrayList<>();
    private final StructureManager structureManager;
    private final SongOfPowerManager songOfPowerManager;
    public MatchManager(PlayerHealthManager healthManager, TeamManager teamManager, AbilityManager abilityManager,
                        Location lobbySpawn, RatingManager ratingManager, DebuffManager debuffManager, OrbitShieldManager orbitShieldManager,
                        CooldownManager cooldownManager, UsageManager usageManager, StatsManager statsManager, LobbyScoreboardManager lobbyScoreboardManager, DamageBuffManager damageBuffManager,
                        ArmorTierManager armorTierManager, HatSelectionManager hatSelectionManager, BroodMotherEntityManager broodMotherEntityManager, Plugin plugin,ArenaMapManager arenaMapManager, StructureManager structureManager,
                        SongOfPowerManager songOfPowerManager) {
        this.healthManager = healthManager;
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
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
        this.broodMotherEntityManager = broodMotherEntityManager;
        this.plugin = plugin;
        this.arenaMapManager = arenaMapManager;
        this.structureManager = structureManager;
        this.songOfPowerManager = songOfPowerManager;
    }


    public void onPlayerEliminated(Player player) {
        Match match = activeMatches.get(player.getUniqueId());
        if (match == null) return;

        Player killer = null;
        UUID attackerId = healthManager.getLastAttacker(player);
        if (attackerId != null) {
            killer = Bukkit.getPlayer(attackerId);
            if (killer != null && killer.isOnline() && !killer.equals(player)) {
                statsManager.addKill(killer);
                killer.sendMessage(String.format("§3You killed §c%s§3!", player.getName()));
            }
        }
        statsManager.addDeath(player);

        if (killer != null && killer.isOnline() && !killer.equals(player)) {
            player.sendMessage(String.format("§3You were killed by §c%s§3!", killer.getName()));
        } else {
            player.sendMessage("§3You were killed!");
        }

        for (Player viewer : match.getAllPlayers()) {
            if (viewer.equals(player)) continue;
            if (viewer.equals(killer)) continue;

            String victimColor = teamManager.isAlly(viewer, player) ? "§a" : "§c";
            String victimName = victimColor + player.getName();

            if (killer != null && killer.isOnline()) {
                String killerColor = teamManager.isAlly(viewer, killer) ? "§a" : "§c";
                String killerName = killerColor + killer.getName();
                viewer.sendMessage(String.format("%s §3was killed by %s§3!", victimName, killerName));
            } else {
                viewer.sendMessage(String.format("%s §3was killed!", victimName));
            }
        }

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        org.bukkit.entity.Firework fw = loc.getWorld().spawn(loc, org.bukkit.entity.Firework.class);
        org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .with(org.bukkit.FireworkEffect.Type.BALL)
                .withColor(org.bukkit.Color.RED)
                .build());
        fw.setFireworkMeta(meta);
        fw.detonate();

        org.bukkit.Material[] materials = {
                org.bukkit.Material.PORKCHOP, org.bukkit.Material.PORKCHOP,
                org.bukkit.Material.BONE, org.bukkit.Material.BONE,
                org.bukkit.Material.POPPY, org.bukkit.Material.POPPY
        };

        java.util.Random random = new java.util.Random();
        for (org.bukkit.Material mat : materials) {
            org.bukkit.entity.Item item = loc.getWorld().dropItem(loc, new org.bukkit.inventory.ItemStack(mat, 1));

            double vx = (random.nextDouble() - 0.5) * 0.5;
            double vy = 0.75 + (random.nextDouble() * 0.15);
            double vz = (random.nextDouble() - 0.5) * 0.5;

            item.setVelocity(new org.bukkit.util.Vector(vx, vy, vz));
        }

        player.setGameMode(GameMode.SPECTATOR);
        debuffManager.clear(player);
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
            winner.sendMessage(String.format("§6Your new rating is %.0f (+%.2f)", ratingManager.getRating(winner), gain));
        }

        for (Player loser : losers) {
            if (!loser.isOnline()) continue;
            // Uses the loser's own rating against the opponent team's average
            double loss = ratingManager.calculateLoss(ratingManager.getRating(loser), winnersAvg);
            ratingManager.applyDelta(loser, loss);
            loser.sendMessage(String.format("§6Your new rating is %.0f (%.2f)", ratingManager.getRating(loser), loss));
        }
    }

    public void cleanupPlayer(Player player) {
        activeMatches.remove(player.getUniqueId());
        teamManager.clear(player);
        debuffManager.clear(player);
        abilityManager.clearAbilities(player);
        damageBuffManager.clear(player);
        cooldownManager.clearPlayer(player);
        usageManager.resetPlayer(player);
        orbitShieldManager.clear(player);
        broodMotherEntityManager.clearAll();
        songOfPowerManager.clear(player);
        EntityCleanupUtils.sweepArenaEntities(player.getWorld());
        if (!player.isOnline()) return;

        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setFoodLevel(20);
        healthManager.setMaxHealth(player, 20.0);
        player.setLevel(0);
        player.setExp(0f);
        player.teleport(lobbySpawn);

        LobbyKit.giveLobbyKit(player, armorTierManager);
        lobbyScoreboardManager.show(player);
        HatEquipUtils.applyEquippedHat(player, hatSelectionManager);
    }

    public void registerMatch(Match match, BukkitTask scoreboardTask) {
        for (Player player : match.getAllPlayers()) {
            activeMatches.put(player.getUniqueId(), match);
        }
        scoreboardTasks.put(match, scoreboardTask);
        activeMatchesList.add(match);
    }

    /** Returns the match a specific player is currently in, or null. */
    public Match getMatchFor(Player player) {
        return activeMatches.get(player.getUniqueId());
    }

    /** Returns all matches currently running (needed for tasks that iterate every active game). */
    public List<Match> getActiveMatches() {
        return new ArrayList<>(activeMatchesList);
    }

    private void endMatch(Match match, String winnerTeam) {
        if (match.isEnded()) return;
        match.setEnded(true);
        List<Player> winners = winnerTeam.equals("RED") ? match.getRed() : match.getBlue();
        List<Player> losers = winnerTeam.equals("RED") ? match.getBlue() : match.getRed();

        for (Player viewer : match.getAllPlayers()) {
            if (!viewer.isOnline()) continue;
            viewer.sendMessage("§6#§7--------------------------§6#");
            for (Player winner : winners) {
                viewer.sendMessage(String.format("§6%s has won the game!", winner.getName()));
            }
            viewer.sendMessage("§6#§7--------------------------§6#");
        }

        applyRatingChanges(winners, losers);

        for (Player winner : winners) {
            if (winner.isOnline()) {
                int earnedCoins = statsManager.addWin(winner);
                winner.sendMessage(String.format("§6You earned a total of %d Coins!", earnedCoins));
            }
        }
        for (Player loser : losers) {
            if (loser.isOnline()) {
                int earnedCoins = statsManager.addLoss(loser);
                loser.sendMessage(String.format("§6You earned a total of %d Coins!", earnedCoins));
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : match.getAllPlayers()) {
                cleanupPlayer(player);
            }
            finishMatch(match);
        }, 140L);
    }



    /** Ends the match in a draw due to the 10-minute time limit. No rating changes are applied. */
    public void endMatchAsDraw(Match match) {
        if (match.isEnded()) return;
        match.setEnded(true);

        for (Player player : match.getAllPlayers()) {
            if (player.isOnline()) {
                player.sendMessage("§6#§7--------------------------§6#");
                player.sendMessage("§6Draw! Time limit reached.");
                player.sendMessage("§6#§7--------------------------§6#");
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : match.getAllPlayers()) {
                cleanupPlayer(player);
            }
            finishMatch(match);
        }, 140L);
    }

    private void finishMatch(Match match) {

        BukkitTask task = scoreboardTasks.remove(match);
        if (task != null) task.cancel();

        match.getPowerupManager().clear();

        activeMatchesList.remove(match);
        arenaMapManager.releaseMap(match.getArenaMap());

        if (!match.getAllPlayers().isEmpty()) {
            EntityCleanupUtils.sweepArenaEntities(match.getAllPlayers().getFirst().getWorld());
        }
        structureManager.clearAll();



    }

    public boolean isInMatch(Player player) {
        return activeMatches.containsKey(player.getUniqueId());
    }
}