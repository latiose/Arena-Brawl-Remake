package org.latios.arenaBrawl.lobby;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.abilities.AbilityManager;
import org.latios.arenaBrawl.cosmetics.ArmorTierManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.CollisionUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.hats.HatEquipUtils;
import org.latios.arenaBrawl.hats.HatSelectionManager;
import org.latios.arenaBrawl.hats.MagicalChestHologramListener;
import org.latios.arenaBrawl.team.TeamManager;

public class LobbyJoinListener implements Listener {

    private final MatchManager matchManager;
    private final TeamManager teamManager;
    private final AbilityManager abilityManager;
    private final DebuffManager debuffManager;
    private final LobbyScoreboardManager lobbyScoreboardManager;
    private final ArmorTierManager armorTierManager;
    private final HatSelectionManager hatSelectionManager;
    private final MagicalChestHologramListener hologramListener;
    private final Plugin plugin;
    private final PlayerHealthManager healthManager;
    public LobbyJoinListener(
            MatchManager matchManager,
            TeamManager teamManager,
            AbilityManager abilityManager,
            DebuffManager debuffManager,
            LobbyScoreboardManager lobbyScoreboardManager,
            ArmorTierManager armorTierManager,
            HatSelectionManager hatSelectionManager,
            MagicalChestHologramListener hologramListener,
            PlayerHealthManager healthManager,
            Plugin plugin) {

        this.matchManager = matchManager;
        this.teamManager = teamManager;
        this.abilityManager = abilityManager;
        this.debuffManager = debuffManager;
        this.lobbyScoreboardManager = lobbyScoreboardManager;
        this.armorTierManager = armorTierManager;
        this.hatSelectionManager = hatSelectionManager;
        this.hologramListener = hologramListener;
        this.healthManager = healthManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (matchManager.isInMatch(player)) {
            matchManager.handleMidMatchReconnect(player);
            return;
        }

        Location lobbySpawn = getLobbySpawnLocation();
        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }

        resetToLobbyState(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !matchManager.isInMatch(player)) {
                lobbyScoreboardManager.show(player);
                hologramListener.scanAllWorlds();
            }
        }, 2L);
    }

    private Location getLobbySpawnLocation() {
        FileConfiguration config = plugin.getConfig();

        String worldName = config.getString("worlds.lobby", "world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            return matchManager.getLobbySpawn();
        }

        double x = config.getDouble("lobby-spawn.x", 0.5);
        double y = config.getDouble("lobby-spawn.y", -60.0);
        double z = config.getDouble("lobby-spawn.z", 0.5);
        float yaw = (float) config.getDouble("lobby-spawn.yaw", 0.0);
        float pitch = (float) config.getDouble("lobby-spawn.pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private void resetToLobbyState(Player player) {
        teamManager.clear(player);
        debuffManager.clear(player);
        abilityManager.clearAbilities(player);

        player.setGameMode(GameMode.SURVIVAL);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setFoodLevel(20);
        player.setHealth(20.0);
        player.setLevel(0);
        player.setExp(0f);
        player.getInventory().clear();

        LobbyKit.giveLobbyKit(player, armorTierManager);
        HatEquipUtils.applyEquippedHat(player, hatSelectionManager);
        CollisionUtils.disableCollision(player);
    }
}