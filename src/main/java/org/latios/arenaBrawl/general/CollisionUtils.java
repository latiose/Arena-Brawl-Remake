// general/CollisionUtils.java
package org.latios.arenaBrawl.general;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class CollisionUtils {

    private static final String NO_COLLISION_TEAM = "no_collision";

    private static Team getOrCreateTeam(Scoreboard scoreboard) {
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team == null) {
            team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }



    public static void disableCollision(Entity entity, Scoreboard scoreboard) {
        Team team = getOrCreateTeam(scoreboard);
        team.addEntry(entity.getUniqueId().toString());
    }

    public static void disableCollision(Player player) {
        Scoreboard board = player.getScoreboard();
        Team team = board.getTeam(NO_COLLISION_TEAM);

        if (team == null) {
            team = board.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }

        team.addEntry(player.getName());
    }

    public static void disableCollisionOnMainScoreboard(Player player) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = mainBoard.getTeam(NO_COLLISION_TEAM);
        if (team == null) {
            team = mainBoard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.addEntry(player.getName());
    }
}