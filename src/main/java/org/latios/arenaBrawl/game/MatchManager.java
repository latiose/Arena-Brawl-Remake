
package org.latios.arenaBrawl.game;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.latios.arenaBrawl.abilities.AbilityManager;

import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.offensive.SkeletonEntityManager;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.abilities.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.LifeLeechManager;
import org.latios.arenaBrawl.abilities.support.SongOfPowerManager;
import org.latios.arenaBrawl.abilities.ultimate.BroodMotherEntityManager;
import org.latios.arenaBrawl.abilities.ultimate.GuardianColossusEntityManager;
import org.latios.arenaBrawl.abilities.ultimate.SoulLinkManager;
import org.latios.arenaBrawl.abilities.UsageManager;

import org.latios.arenaBrawl.abilities.ultimate.ZombieEntityManager;
import org.latios.arenaBrawl.abilities.utility.ShadowCloakManager;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.cosmetics.SwordSelectionManager;
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

import static org.latios.arenaBrawl.general.ParticleUtils.spawnBloodEffect;

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
    private  SwordSelectionManager swordSelectionManager;
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
    private final LifeLeechManager lifeLeechManager;
    private final ShadowCloakManager shadowCloakManager;
    private final SoulLinkManager soulLinkManager;
    private final GuardianColossusEntityManager guardianColossusEntityManager;

    private static final String SEPARATOR_LINE = "§a§l---------------------------------------------";
    public MatchManager(PlayerHealthManager healthManager, TeamManager teamManager, AbilityManager abilityManager,
                        Location lobbySpawn, RatingManager ratingManager, DebuffManager debuffManager, OrbitShieldManager orbitShieldManager,
                        CooldownManager cooldownManager, UsageManager usageManager, StatsManager statsManager, LobbyScoreboardManager lobbyScoreboardManager, DamageBuffManager damageBuffManager,
                        ArmorTierManager armorTierManager, HatSelectionManager hatSelectionManager, BroodMotherEntityManager broodMotherEntityManager, Plugin plugin, ArenaMapManager arenaMapManager, StructureManager structureManager,
                        SongOfPowerManager songOfPowerManager, DrawVoteManager drawVoteManager, ZombieEntityManager zombieEntityManager, SkeletonEntityManager skeletonEntityManager, LifeLeechManager lifeLeechManage, SwordSelectionManager swordSelectionManager,
                        ShadowCloakManager shadowCloakManager, SoulLinkManager soulLinkManager, GuardianColossusEntityManager guardianColossusEntityManager) {
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
        this.lifeLeechManager = lifeLeechManage;
        this.swordSelectionManager = swordSelectionManager;
        this.shadowCloakManager = shadowCloakManager;
        this.soulLinkManager = soulLinkManager;
        this.guardianColossusEntityManager = guardianColossusEntityManager;
    }


    public void onPlayerEliminated(Player player) {
        Match match = activeMatches.get(player.getUniqueId());
        if (match == null) return;

        UUID attackerId = healthManager.getLastAttacker(player);
        Player killer = attackerId != null ? findParticipant(match, attackerId) : null;
        boolean hasKiller = killer != null && !killer.getUniqueId().equals(player.getUniqueId());

        if (hasKiller) {
            statsManager.addKill(killer.getUniqueId());
            if (killer.isOnline()) {
                killer.sendMessage(String.format("§3You killed §c%s§3!", player.getName()));
            }
        }
        statsManager.addDeath(player);

        if (hasKiller) {
            player.sendMessage(String.format("§3You were killed by §c%s§3!", killer.getName()));
        } else {
            player.sendMessage("§3You were killed!");
        }

        for (Player viewer : match.getAllPlayers()) {
            if (viewer.equals(player)) continue;
            if (killer != null && viewer.equals(killer)) continue;

            String victimColor = teamManager.isAlly(viewer, player) ? "§a" : "§c";
            String victimName = victimColor + player.getName();

            if (hasKiller) {
                String killerColor = teamManager.isAlly(viewer, killer) ? "§a" : "§c";
                String killerName = killerColor + killer.getName();
                viewer.sendMessage(String.format("%s §3was killed by %s§3!", victimName, killerName));
            } else {
                viewer.sendMessage(String.format("%s §3was killed!", victimName));
            }
        }

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 10.0f, 1.0f);
        org.bukkit.entity.Firework fw = loc.getWorld().spawn(loc, org.bukkit.entity.Firework.class);
        org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .with(org.bukkit.FireworkEffect.Type.BALL)
                .withColor(org.bukkit.Color.RED)
                .build());
        fw.setFireworkMeta(meta);
        fw.detonate();
        spawnBloodEffect(loc);
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
        shadowCloakManager.clear(player);
        soulLinkManager.clear(player);
        Team winner = match.eliminate(player);
        if (winner != null) {
            endMatch(match, winner);
        }
    }



    private Map<UUID, RatingChange> applyRatingChanges(List<Player> winners, List<Player> losers) {
        Map<UUID, RatingChange> changes = new HashMap<>();
        double losersAvg = losers.stream().mapToDouble(p -> ratingManager.getRating(p.getUniqueId())).average().orElse(1000.0);
        double winnersAvg = winners.stream().mapToDouble(p -> ratingManager.getRating(p.getUniqueId())).average().orElse(1000.0);

        for (Player winner : winners) {
            UUID id = winner.getUniqueId();
            double gain = ratingManager.calculateGain(ratingManager.getRating(id), losersAvg);
            ratingManager.applyDelta(id, gain);
            changes.put(id, new RatingChange(gain, floorRating(id)));
        }

        for (Player loser : losers) {
            UUID id = loser.getUniqueId();
            double loss = ratingManager.calculateLoss(ratingManager.getRating(id), winnersAvg);
            ratingManager.applyDelta(id, loss);
            changes.put(id, new RatingChange(loss, floorRating(id)));
        }
        return changes;
    }

    private long floorRating(Player player) {
        return floorRating(player.getUniqueId());
    }

    private long floorRating(UUID id) {
        return (long) Math.floor(ratingManager.getRating(id));
    }

    /**
     * Resolves the player behind an id among the match participants (online or offline),
     * falling back to any currently online player.
     */
    private Player findParticipant(Match match, UUID id) {
        for (Player participant : match.getAllPlayers()) {
            if (participant.getUniqueId().equals(id)) return participant;
        }
        return Bukkit.getPlayer(id);
    }



    public void announceMatchStart(Match match) {
        List<Team> teams = new ArrayList<>(match.getTeams().keySet());
        for (Player viewer : match.getAllPlayers()) {
            if (!viewer.isOnline()) continue;
            viewer.sendMessage(SEPARATOR_LINE);
            for (int i = 0; i < teams.size(); i++) {
                StringBuilder line = new StringBuilder();
                List<Player> players = match.getTeamPlayers(teams.get(i));
                for (int j = 0; j < players.size(); j++) {
                    Player p = players.get(j);
                    if (j > 0) line.append("  ");
                    line.append(String.format("§e%s §b%d", p.getName(), floorRating(p)));
                }
                viewer.sendMessage(center(line.toString()));
                if (i < teams.size() - 1) {
                    viewer.sendMessage(center("§a§lVS"));
                }
            }
            viewer.sendMessage(SEPARATOR_LINE);
        }
    }

    private void announceMatchResult(Match match, List<Player> winners, List<Player> losers,
                                     Map<UUID, RatingChange> changes) {
        for (Player viewer : match.getAllPlayers()) {
            if (!viewer.isOnline()) continue;
            viewer.sendMessage(SEPARATOR_LINE);
            viewer.sendMessage(center("§a§lVICTORY"));
            for (Player player : winners) {
                viewer.sendMessage(center(resultLine(match, player, changes.get(player.getUniqueId()))));
            }
            viewer.sendMessage(center("§c§lDEFEAT"));
            for (Player player : losers) {
                viewer.sendMessage(center(resultLine(match, player, changes.get(player.getUniqueId()))));
            }
            viewer.sendMessage(SEPARATOR_LINE);
        }
    }

    private String resultLine(Match match, Player player, RatingChange change) {
        long rating = change == null ? floorRating(player) : change.rating();
        String delta = (change == null || change.delta() == 0.0)
                ? ""
                : String.format(Locale.US, " §b(%+.2f)", change.delta());
        return String.format("§c%s§7/§a%s §e%s §b%d%s",
                formatK(match.getDamageDealt(player)),
                formatK(match.getHealingDone(player)),
                player.getName(), rating, delta);
    }

    /** 950 -> "950", 2700 -> "2.7k", 4000 -> "4k" */
    private String formatK(double value) {
        long v = Math.round(value);
        if (v < 1000) return String.valueOf(v);
        double k = Math.round(v / 100.0) / 10.0;
        return (k == Math.floor(k) ? String.valueOf((long) k) : String.valueOf(k)) + "k";
    }

    private static final int CHAT_CENTER_PX = 160;

    private String center(String message) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '§' && i + 1 < message.length()) {
                char code = Character.toLowerCase(message.charAt(++i));
                if (code == 'l') bold = true;
                else if (code == 'r' || "0123456789abcdef".indexOf(code) >= 0) bold = false;
                continue;
            }
            width += charWidth(c) + (bold && c != ' ' ? 1 : 0);
        }
        int spaces = (CHAT_CENTER_PX - width / 2) / 4;
        if (spaces <= 0) return message;
        return " ".repeat(spaces) + message;
    }

    private int charWidth(char c) {
        return switch (c) {
            case 'i', '!', '.', ',', ':', ';', '|', '\'' -> 2;
            case 'l' -> 3;
            case 'I', 't', ' ', '[', ']' -> 4;
            case 'f', 'k', '(', ')', '*', '"', '<', '>' -> 5;
            default -> 6;
        };
    }


    private record RatingChange(double delta, long rating) {
    }

    public void cleanupPlayer(Player player) {
        cleanupPlayerHub(player);
        EntityCleanupUtils.sweepArenaEntities(player.getWorld());
        broodMotherEntityManager.clearForPlayer(player);
        zombieEntityManager.clearForPlayer(player);
        skeletonEntityManager.clearForPlayer(player);
        guardianColossusEntityManager.clearForPlayer(player);
    }

    public void cleanupPlayerHub(Player player) {
        activeMatches.remove(player.getUniqueId());
        teamManager.clear(player);
        debuffManager.clear(player);
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.POISON);
        }
        abilityManager.clearAbilities(player);
        damageBuffManager.clear(player);
        cooldownManager.clearPlayer(player);
        usageManager.resetPlayer(player);
        orbitShieldManager.clear(player);
        songOfPowerManager.clear(player);
        lifeLeechManager.clear(player);
        shadowCloakManager.clear(player);
        soulLinkManager.clear(player);
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

    /**
     * Players participating in the same match as the given player.
     *
     * <p>Use this for any effect that is meant to be "global" (Time Stop,
     * Myriad Truths, ...). Iterating {@code Bukkit.getOnlinePlayers()} or every
     * player in a world would leak the effect into other simultaneous matches.</p>
     *
     * @return the match participants, or an empty list when the player is not in a match.
     */
    public List<Player> getMatchPlayers(Player player) {
        Match match = getMatchFor(player);
        return match != null ? match.getAllPlayers() : List.of();
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
        clearRemainingDebuffs(match);
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
            int earnedCoins = statsManager.addWin(winner);
            if (winner.isOnline()) {
                winner.sendMessage(String.format("§6You earned a total of %d Coins!", earnedCoins));
            }
        }
        for (Player loser : losers) {
            int earnedCoins = statsManager.addLoss(loser);
            if (loser.isOnline()) {
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
        clearRemainingDebuffs(match);
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
        broodMotherEntityManager.clearForMatch(match);
        zombieEntityManager.clearForMatch(match);
        skeletonEntityManager.clearForMatch(match);
        guardianColossusEntityManager.clearForMatch(match);

        if (match.getArenaMap() != null) {
            EntityCleanupUtils.sweepArenaEntities(match.getArenaMap().getWorld());
        } else if (lobbySpawn != null) {
            EntityCleanupUtils.sweepArenaEntities(lobbySpawn.getWorld());
        }
        structureManager.clearForMatch(match);
        activeMatchesList.remove(match);
        arenaMapManager.releaseMap(match.getArenaMap());
    }


    private void clearRemainingDebuffs(Match match) {
        for (Player player : match.getAllPlayers()) {
            if (player.isOnline()) {
                debuffManager.clear(player);
            }
        }
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