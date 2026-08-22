
package org.latios.arenaBrawl.general;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

public class NametagManager {

    private static final String[] TEAM_SLOTS = {"nt_slot0", "nt_slot1", "nt_slot2"};

    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public NametagManager(TeamManager teamManager, PlayerHealthManager healthManager) {
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    /** Updates every viewer's relative nametag colors + health suffix for the other 3 players in the match. */
    public void update(Match match) {
        List<Player> all = match.getAllPlayers();

        for (Player viewer : all) {
            Scoreboard board = match.getIndividualScoreboards().get(viewer);
            if (board == null) continue;

            List<Player> others = all.stream().filter(p -> !p.equals(viewer)).toList();

            for (int i = 0; i < TEAM_SLOTS.length; i++) {
                Team team = board.getTeam(TEAM_SLOTS[i]);
                if (team == null) {
                    team = board.registerNewTeam(TEAM_SLOTS[i]);
                }

                if (i >= others.size()) continue; // fewer than 3 other players, e.g. mid-cleanup

                Player target = others.get(i);
                boolean ally = teamManager.isAlly(viewer, target);

                team.setColor(ally ? ChatColor.GREEN : ChatColor.RED);

                for (String entry : new ArrayList<>(team.getEntries())) {
                    team.removeEntry(entry);
                }
                team.addEntry(target.getName());

                int hp = (int) healthManager.getHealth(target);
                //team.suffix(Component.text(" §f- " + (ally ? "§a" : "§c") + hp + " ❤"));
                team.suffix(
                        Component.text(" ", NamedTextColor.WHITE)
                                .append(Component.text(hp + " ❤", ally ? NamedTextColor.GREEN : NamedTextColor.RED))
                );
            }
        }
    }
}