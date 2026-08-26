
package org.latios.arenaBrawl.general;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class CollisionUtils {

    private static final String NO_COLLISION_TEAM = "no_collision";

    /** Registers the given player as a no-collision entry on ONE specific scoreboard. */
    private static void addEntryToBoard(Scoreboard board, String entryName) {
        Team team = board.getTeam(NO_COLLISION_TEAM);
        if (team == null) {
            team = board.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.addEntry(entryName);
    }

    /**
     * For lobby use: only one player's own scoreboard needs to know about itself,
     * IF every lobby player also has every other lobby player added — but since lobby
     * players constantly join/leave, the simplest robust fix is the same cross-registration
     * approach used for matches (see disableCollisionForGroup below).
     */
    public static void disableCollision(Player player) {
        addEntryToBoard(player.getScoreboard(), player.getName());
    }

    /**
     * Registers EVERY player in the group as a no-collision entry on EVERY player's
     * individual scoreboard, so collision is disabled from every viewer's perspective,
     * not just the entity's own client.
     */
    public static void disableCollisionForGroup(List<Player> players) {
        for (Player viewer : players) {
            Scoreboard viewerBoard = viewer.getScoreboard();
            for (Player target : players) {
                addEntryToBoard(viewerBoard, target.getName());
            }
        }
    }


    private static Team getOrCreateTeam(Scoreboard scoreboard) {
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team == null) {
            team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }

    public static void disableCollision(Entity entity, Scoreboard scoreboard) {
        if (entity instanceof LivingEntity living) {
            living.setCollidable(false);
        }
        Team team = getOrCreateTeam(scoreboard);
        team.addEntry(entity.getUniqueId().toString());
    }
}