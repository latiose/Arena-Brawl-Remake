package org.latios.arenaBrawl.abilities;



import org.bukkit.entity.Player;

import java.util.List;

public interface Ability {
        String getName();
        AbilityCost getCost();
        boolean activate(Player player);
        default void onMatchStart(Player player) {}
        default String getDescription() {
                return "No description available.";
        }
        default List<AbilityStat> getStats() {
                return List.of();
        }
}