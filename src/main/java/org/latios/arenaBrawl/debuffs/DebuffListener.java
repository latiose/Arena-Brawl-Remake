package org.latios.arenaBrawl.debuffs;

import org.bukkit.entity.Player;

public interface DebuffListener {
    // Called right after a debuff of this type is successfully applied
    default void onApplied(Player player, DebuffType type) {}

    // Called when a debuff expires naturally OR is cleared manually (e.g. broken by melee hit)
    default void onExpired(Player player, DebuffType type) {}
}