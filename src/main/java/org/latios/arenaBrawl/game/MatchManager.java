
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
import org.latios.arenaBrawl.abilities.offensive.SkeletonEntityManager;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.SongOfPowerManager;
import org.latios.arenaBrawl.abilities.ultimate.BroodMotherEntityManager;
import org.latios.arenaBrawl.abilities.UsageManager;

import org.latios.arenaBrawl.abilities.ultimate.ZombieEntityManager;
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
import org.latios.arenaBrawl.team.Team;

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
    private final DrawVoteManager drawVoteManager;
    private final ZombieEntityManager zombieEntityManager;
    private final SkeletonEntityManager skeletonEntityManager;
    public MatchManager(PlayerHealthManager healthManager, TeamManager teamManager, AbilityManager abilityManager,
                        Location lobbySpawn, RatingManager ratingManager, DebuffManager debuffManager, OrbitShieldManager orbitShieldManager,
                        CooldownManager cooldownManager, UsageManager usageManager, StatsManager statsManager, LobbyScoreboardManager lobbyScoreboardManager, DamageBuffManager damageBuffManager,
                        ArmorTierManager armorTierManager, HatSelectionManager hatSelectionManager, BroodMotherEntityManager broodMotherEntityManager, Plugin plugin,ArenaMapManager arenaMapManager, StructureManager structureManager,
                        SongOfPowerManager songOfPowerManager, DrawVoteManager drawVoteManager,ZombieEntityManager zombieEntityManager, SkeletonEntityManager skeletonEntityManager) {
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
        this.drawVoteManager = drawVoteManager;
        this.zombieEntityManager = zombieEntityManager;
        this.skeletonEntityManager = skeletonEntityManager;
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
        player.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 15.0f, 2.0f);
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
        Team winner = match.eliminate(player);
        if (winner != null) {
            endMatch(match, winner);
        }
    }



    private Map<UUID, RatingChange> applyRatingChanges(List<Player> winners, List<Player> losers) {
        Map<UUID, RatingChange> changes = new HashMap<>();
        double losersAvg = losers.stream().mapToDouble(ratingManager::getRating).average().orElse(1000.0);
        double winnersAvg = winners.stream().mapToDouble(ratingManager::getRating).average().orElse(1000.0);

        for (Player winner : winners) {
            if (!winner.isOnline()) continue;
            double gain = ratingManager.calculateGain(ratingManager.getRating(winner), losersAvg);
            ratingManager.applyDelta(winner, gain);
            changes.put(winner.getUniqueId(), new RatingChange(gain, floorRating(winner)));
        }

        for (Player loser : losers) {
            if (!loser.isOnline()) continue;
            double loss = ratingManager.calculateLoss(ratingManager.getRating(loser), winnersAvg);
            ratingManager.applyDelta(loser, loss);
            changes.put(loser.getUniqueId(), new RatingChange(loss, floorRating(loser)));
        }
        return changes;
    }

    private long floorRating(Player player) {
        return (long) Math.floor(ratingManager.getRating(player));
    }

    public void announceMatchStart(Match match) {
        for (Player viewer : match.getAllPlayers()) {
            if (!viewer.isOnline()) continue;
            viewer.sendMessage("§6#§7--------------------------§6#");
            List<Team> teams = new ArrayList<>(match.getTeams().keySet());
            for (int i = 0; i < teams.size(); i++) {
                Team team = teams.get(i);
                for (Player player : match.getTeamPlayers(team)) {
                    viewer.sendMessage(String.format("%s%s §f%d", teamColor(team), player.getName(), floorRating(player)));
                }
                if (i < teams.size() - 1) {
                    viewer.sendMessage("§6§lVS");
                }
            }
            viewer.sendMessage("§6#§7--------------------------§6#");
        }
    }

    private void announceMatchResult(Match match, List<Player> winners, List<Player> losers,
                                      Map<UUID, RatingChange> changes) {
        for (Player viewer : match.getAllPlayers()) {
            if (!viewer.isOnline()) continue;
            viewer.sendMessage("§6#§7--------------------------§6#");
            viewer.sendMessage(winners.contains(viewer) ? "§a§lVICTORY" : "§c§lDEFEAT");
            for (Player player : winners) {
                sendResultLine(viewer, player, changes.get(player.getUniqueId()), true);
            }
            for (Player player : losers) {
                sendResultLine(viewer, player, changes.get(player.getUniqueId()), false);
            }
            viewer.sendMessage("§6#§7--------------------------§6#");
        }
    }

    private void sendResultLine(Player viewer, Player player, RatingChange change, boolean winner) {
        String delta = change == null ? "" : String.format("§b(%+.2f) ", change.delta());
        long rating = change == null ? floorRating(player) : change.rating();
        String nameColor = winner ? "§a" : "§c";
        Match match = activeMatches.get(player.getUniqueId());
        int damage = match == null ? 0 : (int) Math.round(match.getDamageDealt(player));
        int healing = match == null ? 0 : (int) Math.round(match.getHealingDone(player));
        viewer.sendMessage(String.format("%s%s%s§f %d §c§l%d§7/§a§l%d",
                delta, nameColor, player.getName(), rating, damage, healing));
    }

    private String teamColor(Team team) {
        return switch (team) {
            case RED -> "§c";
            case BLUE -> "§b";
            case GREEN -> "§a";
            case YELLOW -> "§e";
        };
    }

    private record RatingChange(double delta, long rating) {
    }

    public void cleanupPlayer(Player player) {
        cleanupPlayerHub(player);
        EntityCleanupUtils.sweepArenaEntities(player.getWorld());
    }

    public void cleanupPlayerHub(Player player) {
        activeMatches.remove(player.getUniqueId());
        teamManager.clear(player);
        debuffManager.clear(player);
        abilityManager.clearAbilities(player);
        damageBuffManager.clear(player);
        cooldownManager.clearPlayer(player);
        usageManager.resetPlayer(player);
        orbitShieldManager.clear(player);
        broodMotherEntityManager.clearAll();
        zombieEntityManager.clearAll();
        skeletonEntityManager.clearAll();
        songOfPowerManager.clear(player);
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
        CollisionUtils.disableCollision(player);
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

    public void recordDamage(PlayerHealthManager.DamageEvent event) {
        Match match = activeMatches.get(event.attacker().getUniqueId());
        if (match != null && match.contains(event.victim())) {
            match.addDamage(event.attacker(), event.amount());
        }
    }

    public void recordHealing(PlayerHealthManager.HealingEvent event) {
        Match match = activeMatches.get(event.healer().getUniqueId());
        if (match != null && match.contains(event.target())) {
            match.addHealing(event.healer(), event.amount());
        }
    }

    /** Returns all matches currently running (needed for tasks that iterate every active game). */
    public List<Match> getActiveMatches() {
        return new ArrayList<>(activeMatchesList);
    }

    private void endMatch(Match match, Team winnerTeam) {
        if (match.isEnded()) return;
        match.setEnded(true);
        List<Player> winners = match.getTeamPlayers(winnerTeam);
        List<Player> losers = match.getAllPlayers().stream()
                .filter(player -> !winners.contains(player))
                .toList();

        Map<UUID, RatingChange> ratingChanges = Map.of();
        if (match.getType() == MatchType.TEAMS) {
            ratingChanges = applyRatingChanges(winners, losers);
        }
        announceMatchResult(match, winners, losers, ratingChanges);

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
                if (activeMatches.get(player.getUniqueId()) != match) continue;
                cleanupPlayer(player);
            }
            finishMatch(match);
        }, 140L);
    }



    /** Ends the match in a draw due to the 10-minute time limit. No rating changes are applied. */
    public void endMatchAsDraw(Match match) {
        if (match.isEnded()) return;
        match.setEnded(true);
        drawVoteManager.clear(match);
        for (Player player : match.getAllPlayers()) {
            if (player.isOnline()) {
                player.sendMessage("§6#§7--------------------------§6#");
                player.sendMessage("§6Draw! Time limit reached.");
                player.sendMessage("§6#§7--------------------------§6#");
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : match.getAllPlayers()) {
                if (activeMatches.get(player.getUniqueId()) != match) continue;
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

    public void leaveMatch(Player player) {
        Match match = activeMatches.get(player.getUniqueId());
        if (match == null) return;

        if (!match.isEnded() && player.getGameMode() != GameMode.SPECTATOR) {
            onPlayerEliminated(player);
        }

        cleanupPlayerHub(player);
    }
    public boolean isInMatch(Player player) {
        return activeMatches.containsKey(player.getUniqueId());
    }
}