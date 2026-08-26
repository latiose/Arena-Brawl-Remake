
package org.latios.arenaBrawl.lobby;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.latios.arenaBrawl.abilities.AbilityManager;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.CollisionUtils;
import org.latios.arenaBrawl.hats.HatEquipUtils;
import org.latios.arenaBrawl.hats.HatSelectionManager;
import org.latios.arenaBrawl.team.TeamManager;



public class LobbyJoinListener implements Listener {

    private final MatchManager matchManager;
    private final TeamManager teamManager;
    private final AbilityManager abilityManager;
    private final DebuffManager debuffManager;
    private final LobbyScoreboardManager lobbyScoreboardManager;
    private final ArmorTierManager armorTierManager;
    private final HatSelectionManager hatSelectionManager;

    public LobbyJoinListener(MatchManager matchManager, TeamManager teamManager,
                             AbilityManager abilityManager, DebuffManager debuffManager, LobbyScoreboardManager lobbyScoreboardManager,ArmorTierManager armorTierManager,
                             HatSelectionManager hatSelectionManager) {
        this.matchManager = matchManager;
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
        this.debuffManager = debuffManager;
        this.lobbyScoreboardManager = lobbyScoreboardManager;
        this.armorTierManager = armorTierManager;
        this.hatSelectionManager = hatSelectionManager;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (matchManager.isInMatch(player)) {
            return;
        }

        resetToLobbyState(player);
    }

    private void resetToLobbyState(Player player) {
        teamManager.clear(player);
        debuffManager.clear(player);
        abilityManager.clearAbilities(player);

        player.setGameMode(GameMode.SURVIVAL);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0f);

        LobbyKit.giveLobbyKit(player,armorTierManager);
        HatEquipUtils.applyEquippedHat(player, hatSelectionManager);
        lobbyScoreboardManager.show(player);
        CollisionUtils.disableCollision(player);
    }


}