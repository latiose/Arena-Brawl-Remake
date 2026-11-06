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
 * <p>Syntax: /forcestart [team1 players] [: team2 players]</p>
 *
 * <ul>
 *     <li>The sender always joins team 1 (red). Arguments before ":" are
 *     extra red members, arguments after ":" are blue members.</li>
 *     <li>/forcestart            -> 1v0 (just the sender)</li>
 *     <li>/forcestart a          -> 2v0 (sender and a on red)</li>
 *     <li>/forcestart a : b      -> 2v1</li>
 *     <li>/forcestart a b : c d  -> 2v2</li>
 *     <li>/forcestart : b        -> 1v1</li>
 * </ul>
 *
 * <p>Test matches never affect ratings, coins or stats, never end by
 * elimination, draw vote or time limit, and only finish once every
 * participant has left with /hub or disconnected.</p>
 */
public class ForceStartCommand implements CommandExecutor {

    private static final String USAGE =
            "§cUsage: /forcestart [red players] [: blue players] §7"
                    + "(sender joins red: /forcestart = 1v0, /forcestart : b = 1v1, /forcestart a : b c = 2v1)";

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
        boolean blueSide = false;
        for (String arg : args) {
            if (arg.equals(":")) {
                if (blueSide) {
                    sender.sendMessage(USAGE);
                    return true;
                }
                blueSide = true;
                continue;
            }
            if (blueSide) blueNames.add(arg); else redNames.add(arg);
        }

        List<Player> red = new ArrayList<>();
        red.add(player);
        if (!resolveNames(sender, "red", redNames, red)) return true;

        List<Player> blue = new ArrayList<>();
        if (!resolveNames(sender, "blue", blueNames, blue)) return true;

        Set<Player> participants = new LinkedHashSet<>(red);
        participants.addAll(blue);
        if (participants.size() > 4) {
            sender.sendMessage("§cA test match fits at most 4 players (2v2).");
            return true;
        }

        // Don't let matchmaking start a real match around players in a test game.
        for (Player participant : participants) {
            queueManager.leaveQueue(participant);
        }

        arenaManager.startTestMatch(MatchType.TEAMS, List.copyOf(participants));

        sender.sendMessage(String.format(
                "§aStarted test match: §e%dv%d§a. It only ends when everyone uses /hub or disconnects.",
                red.size(), blue.size()));
        return true;
    }

    /** Appends the online players for each name to the team, or reports who is missing. */
    private boolean resolveNames(CommandSender sender, String teamName, List<String> names, List<Player> team) {
        for (String name : names) {
            Player target = Bukkit.getPlayerExact(name);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or not online: §e" + name);
                return false;
            }
            // Duplicate names resolve to the same player; they are deduped
            // when building the participant set.
            team.add(target);
        }
        return true;
    }
}
