package org.latios.arenaBrawl.general;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;

public final class MatchSoundUtils {

    private MatchSoundUtils() {}

    public static void play(AbilityConfig config, Player source, Sound sound, float volume, float pitch) {
        MatchManager matchManager = config.getMatchManager();
        Match match = matchManager != null ? matchManager.getMatchFor(source) : null;

        if (match != null) {
            for (Player p : match.getAllPlayers()) {
                if (p.isOnline()) {
                    p.playSound(p.getLocation(), sound, volume, pitch);
                }
            }
            return;
        }

        source.getWorld().playSound(source.getLocation(), sound, volume, pitch);
    }

    public static void play(MatchManager matchManager, Player source, Sound sound, float volume, float pitch) {
        Match match = matchManager != null ? matchManager.getMatchFor(source) : null;

        if (match != null) {
            for (Player p : match.getAllPlayers()) {
                if (p.isOnline()) {
                    p.playSound(p.getLocation(), sound, volume, pitch);
                }
            }
            return;
        }

        source.getWorld().playSound(source.getLocation(), sound, volume, pitch);
    }
}