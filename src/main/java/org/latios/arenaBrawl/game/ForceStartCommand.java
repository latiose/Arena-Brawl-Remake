package org.latios.arenaBrawl.game;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.queue.QueueManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin command that force-starts a test match.
 *
 * <p>Syntax: /forcestart [opponents] or /forcestart [teammates] : [opponents]</p>
 *
 * <ul>
 *     <li>/forcestart            -> 1v0 (just the sender)</li>
 *     <li>/forcestart a          -> 1v1 (sender vs a)</li>
 *     <li>/forcestart a b        -> 1v2 (sender vs a and b)</li>
 *     <li>/forcestart a :        -> 2v0 (sender and a on red)</li>
 *     <li>/forcestart a : b      -> 2v1</li>
 *     <li>/forcestart a : b c    -> 2v2</li>
 *     <li>/forcestart : b        -> 1v1</li>
 * </ul>
 *
 * <p>Without the ":" separator every listed player is treated as an opponent,
 * so {@code /forcestart player} starts a 1v1. Teammates go before the
 * separator.</p>
 *
 * <p>Test matches never affect ratings, coins or stats, never end by
 * elimination, draw vote or time limit, and only finish once every
 * participant has left with /hub or disconnected.</p>
 */
public class ForceStartCommand implements CommandExecutor {

    private static final String USAGE =
            "§cUsage: /forcestart [opponents] §7or §c/forcestart [teammates] : [opponents] §7"
                    + "(/forcestart a = 1v1, /forcestart a : b = 2v1, /forcestart : a b = 1v2)";

    private final ArenaManager arenaManager;
    private final QueueManager queueManager;

    public ForceStartCommand(ArenaManager arenaManager, QueueManager queueManager) {
        this.arenaManager = arenaManager;
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        List<String> redNames = new ArrayList<>();
        List<String> blueNames = new ArrayList<>();
        int separator = -1;
        for (int i = 0; i < args.length; i++) {
            if (!args[i].equals(":")) continue;
            if (separator >= 0) {
                sender.sendMessage(USAGE);
                return true;
            }
            separator = i;
        }

        if (separator < 0) {
            // No separator: every listed name is an opponent.
            blueNames.addAll(List.of(args));
        } else {
            redNames.addAll(List.of(args).subList(0, separator));
            blueNames.addAll(List.of(args).subList(separator + 1, args.length));
        }

        List<Player> red = new ArrayList<>();
        red.add(player);
        if (!resolveNames(sender, redNames, red)) return true;

        List<Player> blue = new ArrayList<>();
        if (!resolveNames(sender, blueNames, blue)) return true;

        // Deduplicate while preserving order and never put the sender on blue.
        red = new ArrayList<>(new LinkedHashSet<>(red));
        blue = new ArrayList<>(new LinkedHashSet<>(blue));
        blue.removeIf(candidate -> candidate.getUniqueId().equals(player.getUniqueId()));

        for (Player candidate : blue) {
            boolean alsoRed = red.stream()
                    .anyMatch(redPlayer -> redPlayer.getUniqueId().equals(candidate.getUniqueId()));
            if (alsoRed) {
                sender.sendMessage("§c" + candidate.getName() + " can't be on both teams.");
                return true;
            }
        }

        if (red.size() > 2 || blue.size() > 2) {
            sender.sendMessage("§cEach team supports at most 2 players (2v2).");
            return true;
        }

        // A 1v1 needs a duel map; partial/larger test matches use the 2v2 layout.
        MatchType type = red.size() == 1 && blue.size() == 1
                ? MatchType.DUEL
                : MatchType.TEAMS;

        Set<Player> participants = new LinkedHashSet<>(red);
        participants.addAll(blue);

        // Don't let matchmaking start a real match around players in a test game.
        for (Player participant : participants) {
            queueManager.leaveQueue(participant);
        }

        arenaManager.startTestMatch(type, red, blue);

        sender.sendMessage(String.format(
                "§aStarted test match: §c%dv§9%d§a. It only ends when everyone uses /hub or disconnects.",
                red.size(), blue.size()));
        return true;
    }

    /** Appends the online players for each name to the team, or reports who is missing. */
    private boolean resolveNames(CommandSender sender, List<String> names, List<Player> team) {
        for (String name : names) {
            Player target = Bukkit.getPlayerExact(name);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or not online: §e" + name);
                return false;
            }
            team.add(target);
        }
        return true;
    }
}
